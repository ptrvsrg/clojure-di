(ns clojure-di.container.ioc)

;;; --- Реестр и базовые операции ---

(def ^:private initial-registry
  "Начальное состояние реестра компонентов."
  {:components {}
   :injections {}
   :initializers {}
   :destructors {}})

(defn- registry-atom
  "Создает и возвращает атом с начальным состоянием реестра."
  []
  (atom initial-registry))

(def ^:dynamic *registry*
  "Динамическая переменная, хранящая атом с реестром компонентов."
  (registry-atom))

(defn register
  "Регистрирует компонент в реестре."
  ([registry key recipe]
   (register registry key recipe {}))
  ([registry key recipe metadata]
   (-> registry
       (assoc-in [:components key] (assoc metadata :recipe recipe :key key))
       (cond->
        ;; Если у компонента есть конструктор,
        ;; а у его зависимостей нет явных имен, сопоставляем их по позиции.
        (and (:dependencies metadata)
             (not (map? (:dependencies metadata))))
         (assoc-in [:injections key]
                   (zipmap (map (comp symbol name) (:dependencies metadata))
                           (:dependencies metadata)))))))

(defn- unregister
  "Удаляет компонент из реестра."
  [registry key]
  (-> registry
      (update :components dissoc key)
      (update :injections dissoc key)
      (update :initializers dissoc key)
      (update :destructors dissoc key)))

(defn- get-recipe [registry key]
  (get-in registry [:components key :recipe]))

;;; --- Разрешение зависимостей ---

(declare create-instance)
(declare get-instance)

(defn- resolve-dependencies
  [registry dep-map]
  ;; Определение порядка разрешения зависимостей по приоритету
  (let [priority-order (->> dep-map
                            vals
                            (map #(vector % (or (get-in registry [:components % :priority] 0) 0)))
                            (sort-by second >)
                            (map first))
        ;; Создаём временную мапу с уже созданными зависимостями
        resolved-instances (reduce (fn [m key]
                                     (assoc m key (get-instance registry key)))
                                   {}
                                   priority-order)]
    ;; Собираем результат, используя временную мапу
    (reduce-kv
     (fn [m field-sym target-key]
       (assoc m field-sym (get resolved-instances target-key)))
     {}
     dep-map)))

(defn- get-instance
  "Получает экземпляр компонента по ключу, учитывая его жизненный цикл и зависимости."
  [registry key]
  (let [metadata       (get-in registry [:components key])
        lifecycle      (:lifecycle metadata :prototype)
        singleton-atom (:instance metadata)] ; nil для prototype или несуществующего singleton

    (case lifecycle
      :singleton
      ;; Проверяем сначала, есть ли уже сохраненный инстанс в реестре
      (or (when singleton-atom @singleton-atom)
          ;; Если нет, то создаем новый и сохраняем его
          (create-instance registry key metadata))

      :prototype
      ;; Всегда создаем новый инстанс
      (create-instance registry key metadata))))

(defn- create-instance
  "Создает новый экземпляр компонента, внедряя зависимости."
  [registry key metadata]
  (let [lifecycle          (:lifecycle metadata :prototype)
        dependency-map     (get-in registry [:injections key])
        resolved-deps      (resolve-dependencies registry dependency-map)
        recipe             (get-recipe registry key)
        ;; Проверяем, является ли рецепт функцией.
        instance           (if (fn? recipe)
                             (apply recipe (vals resolved-deps))
                             recipe)]

    ;; Применяем пользовательскую инициализацию
    (when-let [init-fn (get-in registry [:initializers key])]
      (init-fn instance))

    ;; Для синглтона мы обновляем атом с инстансом в реестре.
    (case lifecycle
      :singleton
      (do
        (swap! *registry* assoc-in [:components key :instance] (atom instance))
        instance)
      :prototype instance)))

;;; --- Публичное API ---

(defn add-component!
  "Добавляет компонент в глобальный реестр."
  ([key recipe]
   (add-component! key recipe {}))
  ([key recipe metadata]
   (swap! *registry* register key recipe metadata)))

(defn remove-component!
  "Удаляет компонент из глобального реестра."
  [key]
  (swap! *registry* unregister key))

(defn get-instance*
  "Получает экземпляр компонента из глобального реестра по ключу."
  [key]
  (let [registry  @*registry*
        metadata  (get-in registry [:components key])
        lifecycle (:lifecycle metadata :prototype)
        instance  (get-instance registry key)]
    ;; Если это синглтон, сохраняем его в реестре после создания/получения.
    (when (= lifecycle :singleton)
      (swap! *registry* assoc-in [:components key :instance] (atom instance)))
    instance))

(defn set-injection!
  "Явно задает план внедрения зависимостей для компонента.
  injection-map: {:field-symbol 'dependency-key ...}"
  [key injection-map]
  (swap! *registry* assoc-in [:injections key] injection-map))

(defn set-initializer!
  "Устанавливает функцию-инициализатор (аналог @PostConstruct)
  для компонента по ключу."
  [key init-fn]
  (swap! *registry* assoc-in [:initializers key] init-fn))

(defn set-destructor!
  "Устанавливает функцию-деинициализатор (аналог @PreDestroy)
  для компонента по ключу."
  [key destroy-fn]
  (swap! *registry* assoc-in [:destructors key] destroy-fn))

(defn clear-registry!
  "Очищает глобальный реестр."
  []
  (reset! *registry* initial-registry))

;; ------------------------------------------------------------
;; Dependency graph & prioritization (additional features)
;; ------------------------------------------------------------

(def ^:private default-priority
  "Приоритет по умолчанию используется, если в метаданных компонента не указан параметр :priority."
  0)

(defn dependency-graph
  "Создаёт граф зависимостей на основе реестра (или по умолчанию — глобального реестра).

  Возвращает структуру данных:

  {:nodes    #{:a :b ...}
   :adj      {:a [:b :c] ...}          ;; edges a -> deps
   :edges    [{:from :a :to :b} ...]
   :priority {:a 10 :b 0 ...}}"
  ([] (dependency-graph @*registry*))
  ([registry]
   (let [components (-> registry :components keys set)
         injections (:injections registry)
         adj (into {}
                   (for [k components]
                     [k (->> (vals (get injections k {}))
                             distinct
                             vec)]))
         edges (->> injections
                    (mapcat (fn [[from depmap]]
                              (for [to (vals depmap)]
                                {:from from :to to})))
                    vec)
         priority (into {}
                        (for [[k meta] (:components registry)]
                          [k (get meta :priority default-priority)]))]
     {:nodes components
      :adj adj
      :edges edges
      :priority priority})))

(defn- sort-by-priority-desc
  "Сортирует коллекцию ключей компонентов по убыванию приоритета, затем по ключу (стабильное разрешение конфликтов)."
  [priority-map ks]
  (->> ks
       (sort-by (fn [k] [(- (long (get priority-map k default-priority))) (pr-str k)]))))

(defn prioritized-dependencies
  "Возвращает прямые зависимости `component-key`, отсортированные по убыванию приоритета компонента."
  ([component-key] (prioritized-dependencies @*registry* component-key))
  ([registry component-key]
   (let [{:keys [adj priority]} (dependency-graph registry)
         deps (get adj component-key [])]
     (vec (sort-by-priority-desc priority deps)))))

(defn prioritized-instantiation-order
  "Вычисляет порядок создания зависимостей для `root-key` с
  использованием обхода в глубину (DFS) топологической сортировки,
  посещая зависимости каждого узла, отсортированные по убыванию :priority.
  Returns a vector order where dependencies go before dependents.
  Throws (ex-info ...) on cycle with data {:type :cycle :path [...]}."
  ([root-key] (prioritized-instantiation-order @*registry* root-key))
  ([registry root-key]
   (let [{:keys [adj priority]} (dependency-graph registry)]
     (letfn [(dfs [node {:keys [temp perm stack _] :as st}]
               (cond
                 (contains? perm node) st

                 (contains? temp node)
                 (throw (ex-info (str "Dependency cycle detected at " (pr-str node))
                                 {:type :cycle
                                  :path (conj stack node)}))

                 :else
                 (let [st' (-> st
                               (update :temp conj node)
                               (update :stack conj node))
                       deps (sort-by-priority-desc priority (get adj node []))
                       st'' (reduce (fn [acc d] (dfs d acc)) st' deps)]
                   (-> st''
                       (update :temp disj node)
                       (update :perm conj node)
                       (update :stack pop)
                       (update :order conj node)))))]
       (-> (dfs root-key {:temp #{} :perm #{} :stack [] :order []})
           :order
           vec)))))

(defn dependency-plan
  "Удобная обёртка, возвращающая и граф, и порядок создания:
  {:graph <dependency-graph> :order [...]}"
  ([root-key] (dependency-plan @*registry* root-key))
  ([registry root-key]
   {:graph (dependency-graph registry)
    :order (prioritized-instantiation-order registry root-key)}))
