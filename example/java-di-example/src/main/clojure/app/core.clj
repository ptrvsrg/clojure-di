(ns app.core
  (:require [clojure-di.core :as di-core]
            [clojure-di.container.java-injection :as di-jinj])
  (:import (com.example.app UserService)
           (com.example.app.database SimpleDatabase)))

(defonce ^:private user-service-key ::UserService)
(defonce ^:private simple-db-key    ::Database)

(defn register-beans []
  "Регистрирует все Java-компоненты в нашем DI-контейнере."
  (di-core/clear-all!)
  (di-jinj/register-java-bean! SimpleDatabase simple-db-key)
  (di-jinj/register-java-bean! UserService user-service-key))

(defn get-user-service []
  "Публичная функция, которая получает экземпляр `UserService` из контейнера.
   Она будет вызываться из Java-кода."
  (di-core/get-instance ::UserService))

(defn init []
  "Функция для инициализации. Она может сделать что-то еще,
   но главное -- зарегистрировать наши компоненты."
  (register-beans))
