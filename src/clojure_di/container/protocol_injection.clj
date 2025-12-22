(ns clojure-di.container.protocol-injection
  (:require [clojure-di.container.ioc :as ioc]))

;;; --- Хранилище связей Протокол -> Компонент ---

(def ^:private ^:dynamic *protocol-registry*
  "Атом для хранения сопоставления протоколов ключам компонентов."
  (atom {}))

(defn register-protocol-impl
  "Регистрирует, что компонент с `component-key` является реализацией `protocol-sym`."
  [protocol-sym component-key]
  (swap! *protocol-registry* assoc protocol-sym component-key))

(defn get-protocol-impl-key
  "Возвращает ключ компонента, зарегистрированного для данного протокола.
  Если символа нет, предполагаем, что это и есть ключ компонента."
  [protocol-sym-or-key]
  (if-let [registered-key (get @*protocol-registry* protocol-sym-or-key)]
    registered-key
    protocol-sym-or-key))

(defn get-instance-for-protocol
  "Получает экземпляр компонента для заданного протокола или ключа."
  [protocol-sym-or-key]
  (ioc/get-instance* (get-protocol-impl-key protocol-sym-or-key)))

(defn clear-protocol-registry!
  "Очищает реестр протоколов. Полезно для тестов."
  []
  (reset! *protocol-registry* {}))
