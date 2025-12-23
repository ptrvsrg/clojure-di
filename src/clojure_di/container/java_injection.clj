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
   (let [deps (resolve-constructor-dependencies (find-injectable-constructor class))
         bean-fn (fn [& ds]
                   (let [constructor (find-injectable-constructor class)]
                       (.newInstance constructor (into-array Object ds))))]
     ;; 1. Регистрируем компонент под его основным ключом (например, :my-calc)
     (ioc/add-component! component-key bean-fn {:dependencies deps})

     ;; 2. Регистрируем этот компонент как реализацию ВСЕХ его интерфейсов.
     (doseq [interface (.getInterfaces class)]
       ;; 2.1. Создаем алиас в ОСНОВНОМ реестре для разрешения зависимостей в Java-классах
       (ioc/add-component! (keyword (.getName interface)) bean-fn)
       ;; 2.2. Создаем алиас в РЕЕСТРЕ ПРОТОКОЛОВ для разрешения зависимостей в Clojure-компонентах
       (pinj/register-protocol-impl (str (.getName interface)) component-key)))))

