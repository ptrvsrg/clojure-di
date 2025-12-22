(ns clojure-di.core
  (:require [clojure-di.container.ioc :as ioc]
            [clojure-di.container.protocol-injection :as pinj]))

;; Реэкспорт основных функций
(def add-component! ioc/add-component!)
(def get-instance ioc/get-instance*)
(def get-instance-for-protocol pinj/get-instance-for-protocol)
(def register-protocol-impl pinj/register-protocol-impl)

;; Вспомогательный макрос для удобства
(defmacro defcomponent
  "Поверхность для ручного создания компонента.
   `key`: ключ компонента
   `impl`: форма создания компонента (например, ->Record или конструктор)
   `args`: вектор аргументов `impl`
   `opts`: мапа опций `:lifecycle`, `:implements`"
  [key impl args & [opts]]
  `(do
     (ioc/add-component!
      '~key
      (fn [& deps#]
        (let [~(vec args) deps#]
          ~impl))
      ~(dissoc opts :implements))
     (when-let [protocols# (:implements ~opts)]
       (doseq [p# protocols#]
         (pinj/register-protocol-impl p# '~key)))))

(defn clear-all!
  "Очищает все реестры контейнера."
  []
  (ioc/clear-registry!)
  (pinj/clear-protocol-registry!))
