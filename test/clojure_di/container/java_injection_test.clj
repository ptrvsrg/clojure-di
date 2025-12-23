(ns clojure-di.container.java-injection-test
  (:require [clojure-di.container.java-injection :as jinj]
            [clojure-di.container.protocol-injection :as pinj]
            [clojure-di.core :as di]
            [clojure.test :refer :all])
  (:import [com.example.di Calculator DataProcessor SimpleCalculator]))

(jinj/def-java-interface-protocol CalculatorProtocol com.example.di.Calculator)

(extend SimpleCalculator
  CalculatorProtocol
  {:add (fn [this a b] (.add ^Calculator this a b))
   :multiply (fn [this a b] (.multiply ^Calculator this a b))})

(deftest test-java-bean-registration
  (testing "register-java-bean! creates a valid component"
    (di/clear-all!)
    (di/register-java-bean! SimpleCalculator :my-calc)
    (let [bean (di/get-instance :my-calc)]
      (is (instance? SimpleCalculator bean))))

  (testing "register-java-bean! registers the component for its interfaces"
    (di/clear-all!)
    (di/register-java-bean! SimpleCalculator :my-calc)

    ;; Проверяем, что контейнер знает, какой компонент предоставить для интерфейса `Calculator`
    ;; Ключ для разрешения по умолчанию - это полное имя интерфейса (или класса)
    (is (= :my-calc (pinj/get-protocol-impl-key (.getName Calculator))))))

(deftest test-java-dependency-injection
  (testing "can inject a Java bean dependency via its interface"
    (di/clear-all!)
    ;; 1. Регистрируем реализацию. Ключ может быть любым.
    (di/register-java-bean! SimpleCalculator :my-calc)

    ;; 2. Регистрируем клиента, который зависит от интерфейса.
    (di/register-java-bean! DataProcessor :my-processor)

    ;; 3. Запрашиваем клиента.
    (let [processor (di/get-instance :my-processor)]
      (is (instance? DataProcessor processor))
      (is (instance? Calculator (.getCalculator processor)))
      (is (= 11.0 (.process processor 2 3))))) ; (2*3) + (2+3) = 11

  (testing "Java bean can be retrieved by its default key"
    (di/clear-all!)
    (di/register-java-bean! SimpleCalculator)
    ;; Ключ по умолчанию - полное имя класса
    (let [calc (di/get-instance :com.example.di.SimpleCalculator)]
      (is (instance? SimpleCalculator calc))
      (is (= 7.0 (.add calc 3 4))))))

(deftest test-mixed-clojure-java-injection
  (testing "A Clojure component can depend on a Java interface"
    (di/clear-all!)
    ;; 1. Регистрируем Java-реализацию
    (di/register-java-bean! SimpleCalculator :calc-service)

    ;; 2. Определяем Clojure-компонент, который принимает Calculator как зависимость
    (di/add-component! ::clojure-client
                       (fn [calc]
                         {:service calc :type "ClojureComponent"})
                       {:dependencies [:calc-service]})

    ;; 3. Запрашиваем Clojure-компонент. DI должен:
    ;;    - Найти `:clojure-client`
    ;;    - Понять, что нужна зависимость `:calc-service`
    ;;    - Передать `SimpleCalculator` (как `Calculator`) в конструктор `::clojure-client`
    (let [client (di/get-instance ::clojure-client)]
      (is (= "ClojureComponent" (:type client)))
      (is (satisfies? CalculatorProtocol (:service client))))))

(deftest test-protocol-bridge-functionality
  (testing "Clojure code can call protocol methods on a Java implementation"
    (di/clear-all!)
    (di/register-java-bean! SimpleCalculator :calc)
    (let [calc-instance (di/get-instance :calc)]
      (is (satisfies? CalculatorProtocol calc-instance))
      ;; Теперь этот вызов будет работать
      (is (= 5.0 (add calc-instance 2.0 3.0))))))

