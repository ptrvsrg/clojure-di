(ns clojure-di.container.ioc-test
  (:require [clojure.test :refer :all]
            [clojure-di.container.ioc :as ioc :refer [get-instance*]]))

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
