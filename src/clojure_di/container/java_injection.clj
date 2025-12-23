(ns clojure-di.container.java-injection
  (:require [clojure-di.container.ioc :as ioc]
            [clojure-di.container.protocol-injection :as pinj]))

(defn- find-injectable-constructor
  [^Class class]
  (let [constructors (.getConstructors class)
        with-inject (filter #(.isAnnotationPresent % javax.inject.Inject) constructors)]
    (cond
      (seq with-inject) (first with-inject)
      (= 1 (count constructors)) (first constructors)
      :else (throw (IllegalArgumentException.
                    (str "Класс " (.getName class) " должен иметь либо один конструктор, либо конструктор с аннотацией @Inject"))))))

(defn- resolve-constructor-dependencies
  "Определяет типы параметров конструктора и возвращает вектор ключей для их поиска в DI-контейнере."
  [^java.lang.reflect.Constructor constructor]
  (vec (map #(keyword (.getName ^Class %)) (.getParameterTypes constructor))))

(defn register-java-bean!
  "Аналог add-component, но для Java-классов.
   Автоматически регистрирует созданный bean как реализацию всех его интерфейсов."
  ([class] (register-java-bean! class (keyword (.getName class))))
  ([class component-key]
   (let [constructor (find-injectable-constructor class)
         deps (resolve-constructor-dependencies constructor)
         bean-fn (fn [& ds]
                   (.newInstance constructor (into-array Object ds)))]
     ;; 1. Регистрируем компонент под его основным ключом (например, :my-calc)
     (ioc/add-component! component-key bean-fn {:dependencies deps})

     ;; 2. Регистрируем этот компонент как реализацию ВСЕХ его интерфейсов.
     (doseq [interface (.getInterfaces class)]
       ;; 2.1. Создаем алиас в ОСНОВНОМ реестре для разрешения зависимостей в Java-классах
       (ioc/add-component! (keyword (.getName interface)) bean-fn {:dependencies deps})
       ;; 2.2. Создаем алиас в РЕЕСТРЕ ПРОТОКОЛОВ для разрешения зависимостей в Clojure-компонентах
       (pinj/register-protocol-impl (str (.getName interface)) component-key)))))

(defmacro def-java-interface-protocol
  "Создает Clojure-протокол, который служит мостом к Java-интерфейсу.
   `protocol-name`: Имя создаваемого Clojure-протокола.
   `interface-class`: Полное имя класса Java-интерфейса (как символ или строка)."
  [protocol-name interface-class]
  (let [^Class interface (if (string? interface-class)
                           (Class/forName interface-class)
                           (eval interface-class))
        methods          (for [^java.lang.reflect.Method m (.getMethods interface)
                               :let [method-name (symbol (.getName m))
                                     param-count (alength (.getParameterTypes m))
                                     ;; Генерируем аргументы для метода протокола: [this p1 p2 ...]
                                     args        (vec (cons 'this (take param-count (repeatedly gensym))))]]
                           ;; Тело метода: делегируем вызов Java-объекту
                           `(~method-name ~args
                                          (. ~(first args) ~method-name ~@(rest args))))]
    `(defprotocol ~protocol-name
       ~@methods)))
