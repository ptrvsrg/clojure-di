(ns clojure-di.container.ioc-test
  (:require [clojure-di.container.ioc :as ioc :refer [get-instance*]]
            [clojure.test :refer :all]))

;; Вспомогательные "заглушки" для тестов
(defrecord TestRepo [])
(defrecord TestService [repo])
(defrecord TestConfig [value])
(defrecord TestComponent [config service])

(defn make-test-service [repo] (->TestService repo))
(defn make-test-component [config service] (->TestComponent config service))

;; Общая функция для очистки состояния перед каждым тестом
(defn clear-fixture [f]
  (ioc/clear-registry!)
  (f))

(use-fixtures :each clear-fixture)

(deftest test-ioc-container
  (testing "can register and retrieve a simple component"
    (ioc/add-component! ::simple "a-string")
    (is (= "a-string" (get-instance* ::simple))))

  (testing "handles prototype lifecycle by default"
    (ioc/add-component! ::proto (fn [] (java.util.Date.)))
    (let [i1 (get-instance* ::proto)
          i2 (get-instance* ::proto)]
      (is (false? (identical? i1 i2)))))

  (testing "handles singleton lifecycle"
    (ioc/add-component! ::singleton (fn [] (java.util.Date.)) {:lifecycle :singleton})
    (let [i1 (get-instance* ::singleton)
          i2 (get-instance* ::singleton)]
      (is (true? (identical? i1 i2)))))

  (testing "can inject dependencies using positional arguments"
    (ioc/add-component! ::repo (fn [] (->TestRepo)))
    (ioc/add-component! ::service make-test-service {:dependencies [::repo]})
    (let [service-inst (get-instance* ::service)]
      (is (instance? TestRepo (:repo service-inst)))))

  (testing "can use map-based injection"
    (ioc/add-component! ::config (fn [] (->TestConfig "test-value")))
    (ioc/add-component! ::service2 (fn [repo] (->TestService repo)))
    (ioc/set-injection! ::service2 {:repo ::repo})

    (let [service-inst (get-instance* ::service2)]
      (is (instance? TestRepo (:repo service-inst)))))

  (testing "supports custom initializers"
    (ioc/add-component! ::with-init (fn [] (atom nil)))
    (ioc/set-initializer! ::with-init (fn [instance] (reset! instance 42)))
    (let [inst (get-instance* ::with-init)]
      (is (= 42 @inst))))

  (testing "component lifecycle management"
    (ioc/add-component! ::temp (fn [] "temp-value"))
    (ioc/remove-component! ::temp)
    (is (nil? (get-in @ioc/*registry* [:components ::temp])))

    (ioc/add-component! ::temp1 (fn [] "val1"))
    (ioc/add-component! ::temp2 (fn [] "val2") {:lifecycle :singleton})
    (ioc/set-injection! ::temp2 {})
    (ioc/set-initializer! ::temp2 (fn [x] x))
    (ioc/set-destructor! ::temp2 (fn [x] x))
    (ioc/clear-registry!)
    (is (= (count (:components @ioc/*registry*)) 0))
    (is (= (count (:injections @ioc/*registry*)) 0))
    (is (= (count (:initializers @ioc/*registry*)) 0))
    (is (= (count (:destructors @ioc/*registry*)) 0)))

  (testing "clear-registry! does not call destructors for simplicity"
    (ioc/clear-registry!)
    (let [destructor-called? (atom false)]
      (ioc/add-component! ::another (fn [] "val"))
      (ioc/set-destructor! ::another (fn [_] (reset! destructor-called? true)))
      (get-instance* ::another)
      (ioc/clear-registry!)
      (is (false? @destructor-called?) "Destructor should NOT be called on clear-registry!"))))

(deftest test-dependency-graph-structure
  (testing "dependency-graph returns nodes/edges/adj/priority as data"
    (ioc/add-component! ::a (fn [] :a))
    (ioc/add-component! ::b (fn [] :b) {:priority 10})
    (ioc/add-component! ::c (fn [] :c) {:priority 1})

    ;; a depends on b and c
    (ioc/add-component! ::root (fn [b c] {:b b :c c}) {:dependencies [::b ::c]})

    ;; ensure injections are present (dependencies vector -> positional injection)
    (let [g (ioc/dependency-graph)]
      (is (contains? (:nodes g) ::root))
      (is (= 10 (get-in g [:priority ::b])))
      (is (= 0  (get-in g [:priority ::a])) "default priority should be 0 when missing")
      (is (some #{{:from ::root :to ::b}} (:edges g)))
      (is (some #{{:from ::root :to ::c}} (:edges g)))
      (is (= (set [::b ::c]) (set (get-in g [:adj ::root])))))))

(deftest test-prioritized-instantiation-order-and_dependency_creation_order
  (testing "prioritized-instantiation-order respects :priority and dependencies-first"
    (let [created (atom [])]
      ;; leaf deps push into created when instantiated
      (ioc/add-component! ::low  (fn [] (swap! created conj ::low)  :low)  {:priority 1})
      (ioc/add-component! ::high (fn [] (swap! created conj ::high) :high) {:priority 100})

      ;; root depends on both
      (ioc/add-component! ::root
                          (fn [a b]
                            ;; recipe should run after deps
                            (swap! created conj ::root)
                            {:a a :b b})
                          {:dependencies [::low ::high]})

      ;; 1) check computed plan order
      (let [order (ioc/prioritized-instantiation-order ::root)]
        ;; must contain all three
        (is (= #{::low ::high ::root} (set order)))
        ;; deps first, root last
        (is (= ::root (last order))))

      ;; 2) check actual creation order of deps respects priority (high before low),
      ;; because resolve-dependencies in ioc.clj does get-instance calls in that order.
      (ioc/get-instance* ::root)
      (is (= [::high ::low ::root] @created)
          "Expected dependency creation order: high-priority dep first, then low, then root"))))

(deftest test-cycle-detection-in_plan
  (testing "prioritized-instantiation-order detects cycles"
    (ioc/add-component! ::a (fn [b] {:b b}) {:dependencies [::b]})
    (ioc/add-component! ::b (fn [a] {:a a}) {:dependencies [::a]})
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Dependency cycle detected"
         (ioc/prioritized-instantiation-order ::a)))))

(deftest test-prioritized-dependencies-ordering
  (testing "prioritized-dependencies sorts direct deps by priority desc"
    (ioc/add-component! ::low  (fn [] :low)  {:priority 1})
    (ioc/add-component! ::mid  (fn [] :mid)  {:priority 10})
    (ioc/add-component! ::high (fn [] :high) {:priority 100})

    ;; root depends on [low high mid] in that order,
    ;; but prioritized-dependencies must return [high mid low]
    (ioc/add-component! ::root
                        (fn [a b c] {:a a :b b :c c})
                        {:dependencies [::low ::high ::mid]})

    (is (= [::high ::mid ::low]
           (ioc/prioritized-dependencies ::root)))))

(deftest test-prioritized-dependencies-default-priority
  (testing "prioritized-dependencies treats missing :priority as 0"
    ;; no priority => 0
    (ioc/add-component! ::no-prio (fn [] :x))
    (ioc/add-component! ::p1      (fn [] :y) {:priority 1})

    (ioc/add-component! ::root
                        (fn [a b] {:a a :b b})
                        {:dependencies [::no-prio ::p1]})

    ;; p1 (1) should go before no-prio (0)
    (is (= [::p1 ::no-prio]
           (ioc/prioritized-dependencies ::root)))))

(deftest test-prioritized-dependencies-unknown-component
  (testing "prioritized-dependencies returns empty vector for unknown component key"
    (is (= [] (ioc/prioritized-dependencies ::does-not-exist)))))

