(ns clojure-di.container.protocol-injection-test
  (:require [clojure.test :refer :all]
            [clojure-di.core :refer :all]))

;; Определяем тестовый протокол и его реализацию
(defprotocol Database
  (query [this q]))

(defrecord InMemoryDb []
  Database
  (query [_ q] (str "Result of: " q)))

(deftest test-protocol-injection
  (testing "can resolve a component by its protocol"
    (clear-all!)
    (add-component! :in-memory-db (fn [] (->InMemoryDb)))
    (register-protocol-impl `Database :in-memory-db)

    (let [db-instance (get-instance-for-protocol `Database)]
      (is (satisfies? Database db-instance))
      (is (= "Result of: SELECT * FROM users" (query db-instance "SELECT * FROM users")))))

  (testing "can use `defcomponent` macro for convenience"
    (clear-all!)
    (defcomponent :cache-db
      (->InMemoryDb)
      []
      {:implements [`Database]})

    (let [db-instance (get-instance-for-protocol `Database)]
      (is (satisfies? Database db-instance)))))
