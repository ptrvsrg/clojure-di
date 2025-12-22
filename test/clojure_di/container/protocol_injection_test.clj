(ns clojure-di.container.protocol-injection-test
  (:require [clojure-di.core :as core]
            [clojure.test :refer :all]))

;; Определяем тестовый протокол и его реализацию
(defprotocol Database
  (query [this q]))

(defrecord InMemoryDb []
  Database
  (query [_ q] (str "Result of: " q)))

(deftest test-protocol-injection
  (testing "can resolve a component by its protocol"
    (core/clear-all!)
    (core/add-component! :in-memory-db (fn [] (->InMemoryDb)))
    (core/register-protocol-impl `Database :in-memory-db)

    (let [db-instance (core/get-instance-for-protocol `Database)]
      (is (satisfies? Database db-instance))
      (is (= "Result of: SELECT * FROM users" (query db-instance "SELECT * FROM users")))))

  (testing "can use `defcomponent` macro for convenience"
    (core/clear-all!)
    (core/defcomponent :cache-db
      (->InMemoryDb)
      []
      {:implements [`Database]})

    (let [db-instance (core/get-instance-for-protocol `Database)]
      (is (satisfies? Database db-instance)))))
