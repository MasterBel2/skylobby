(ns skylobby.fx.import
  (:require
    [cljfx.ext.node :as fx.ext.node]
    [clojure.string :as string]
    skylobby.fx
    [skylobby.resource :as resource]
    [spring-lobby.fs :as fs]
    [spring-lobby.fx.font-icon :as font-icon]
    [spring-lobby.util :as u]
    [taoensso.tufte :as tufte]))


(def import-window-width 1600)
(def import-window-height 800)


(defn import-sources [extra-import-sources]
  (concat
    [{:import-source-name "Spring"
      :file (fs/spring-root)
      :builtin true}
     {:import-source-name "Beyond All Reason"
      :file (fs/bar-root)
      :builtin true}
     {:import-source-name "Zero-K"
      :file (fs/zerok-root)
      :builtin true}]
    extra-import-sources))

(defn- import-type-cell
  [import-type]
  {:text (if import-type
           (name import-type)
           " < nil > ")})

(defn- import-source-cell
  [{:keys [file import-source-name]}]
  {:text (str import-source-name
              (when file
                (str " ( at " (fs/canonical-path file) " )")))})


(def import-window-keys
  [:copying :css :extra-import-sources :file-cache :import-filter :import-source-name :import-type
   :importables-by-path :show-importer :spring-isolation-dir])

(defn import-window-impl
  [{:keys [copying css extra-import-sources file-cache import-filter import-type import-source-name importables-by-path
           screen-bounds
           show-importer spring-isolation-dir tasks-by-type]}]
  (let [import-sources (import-sources extra-import-sources)
        import-source (->> import-sources
                           (filter (comp #{import-source-name} :import-source-name))
                           first)
        importables (->> (or (vals importables-by-path) [])
                         (filter (fn [importable]
                                   (if import-source-name
                                     (= import-source-name (:import-source-name importable))
                                     true)))
                         (filter (fn [{:keys [resource-file resource-name]}]
                                   (if-not (string/blank? import-filter)
                                     (let [path (fs/canonical-path resource-file)]
                                       (or (and path
                                                (string/includes?
                                                  (string/lower-case path)
                                                  (string/lower-case import-filter)))
                                           (and resource-name
                                                (string/includes?
                                                  (string/lower-case resource-name)
                                                  (string/lower-case import-filter)))))
                                     true)))
                         (filter (fn [{:keys [resource-type]}]
                                   (if import-type
                                     (= import-type resource-type)
                                     true))))
        import-tasks (->> (get tasks-by-type :spring-lobby/import)
                          (map (comp fs/canonical-path :resource-file :importable))
                          set)
        {:keys [width height]} screen-bounds]
    {:fx/type :stage
     :showing (boolean show-importer)
     :title (str u/app-name " Importer")
     :icons skylobby.fx/icons
     :on-close-request {:event/type :spring-lobby/dissoc
                        :key :show-importer}
     :width ((fnil min import-window-width) width import-window-width)
     :height ((fnil min import-window-height) height import-window-height)
     :scene
     {:fx/type :scene
      :stylesheets (skylobby.fx/stylesheet-urls css)
      :root
      (if show-importer
        {:fx/type :v-box
         :style {:-fx-font-size 16}
         :children
         [{:fx/type :button
           :text "Refresh All Imports"
           :on-action {:event/type :spring-lobby/scan-imports
                       :sources import-sources}}
          {:fx/type :h-box
           :alignment :center-left
           :children
           (concat
             [{:fx/type :label
               :text " Filter source: "}
              {:fx/type :combo-box
               :value import-source
               :items import-sources
               :button-cell import-source-cell
               :prompt-text " < pick a source > "
               :cell-factory
               {:fx/cell-type :list-cell
                :describe import-source-cell}
               :on-value-changed {:event/type :spring-lobby/import-source-change}
               :tooltip {:fx/type :tooltip
                         :show-delay [10 :ms]
                         :text "Choose import source"}}]
             (when import-source
               [{:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Clear source filter"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/dissoc
                              :key :import-source-name}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-close:16:white"}}}
                {:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Open import source directory"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/desktop-browse-dir
                              :file (:file import-source)}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-folder:16:white"}}}])
             (when import-source
               [{:fx/type :button
                 :text " Refresh "
                 :on-action {:event/type :spring-lobby/add-task
                             :task (merge {:spring-lobby/task-type :spring-lobby/scan-imports}
                                          import-source)}
                 :graphic
                 {:fx/type font-icon/lifecycle
                  :icon-literal "mdi-refresh:16:white"}}]))}
          {:fx/type :h-box
           :alignment :center-left
           :style {:-fx-font-size 16}
           :children
           (concat
             [{:fx/type :label
               :text " Filter: "}
              {:fx/type :text-field
               :text import-filter
               :prompt-text "Filter by name or path"
               :on-text-changed {:event/type :spring-lobby/assoc
                                 :key :import-filter}}]
             (when-not (string/blank? import-filter)
               [{:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Clear filter"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/dissoc
                              :key :import-filter}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-close:16:white"}}}])
             [{:fx/type :label
               :text " Filter type: "}
              {:fx/type :combo-box
               :value import-type
               :items resource/resource-types
               :button-cell import-type-cell
               :prompt-text " < pick a type > "
               :cell-factory
               {:fx/cell-type :list-cell
                :describe import-type-cell}
               :on-value-changed {:event/type :spring-lobby/assoc
                                  :key :import-type}
               :tooltip {:fx/type :tooltip
                         :show-delay [10 :ms]
                         :text "Choose import type"}}]
             (when import-type
               [{:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Clear type filter"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/dissoc
                              :key :import-type}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-close:16:white"}}}]))}
          {:fx/type :label
           :text (str (count importables) " artifacts")}
          {:fx/type :table-view
           :column-resize-policy :constrained ; TODO auto resize
           :v-box/vgrow :always
           :items importables
           :columns
           [{:fx/type :table-column
             :text "Source"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe (fn [i] {:text (str (:import-source-name i))})}}
            {:fx/type :table-column
             :text "Type"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe (comp import-type-cell :resource-type)}}
            {:fx/type :table-column
             :text "Filename"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe (fn [i] {:text (str (:resource-filename i))})}}
            {:fx/type :table-column
             :text "Path"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe (fn [i] {:text (str (:resource-file i))})}}
            {:fx/type :table-column
             :text "Import"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [importable]
                (let [source-path (some-> importable :resource-file fs/canonical-path)
                      dest-path (some->> importable (resource/resource-dest spring-isolation-dir) fs/canonical-path)
                      copying (or (-> copying (get source-path) :status)
                                  (-> copying (get dest-path) :status))
                      in-progress (boolean
                                    (or (contains? import-tasks source-path)
                                        copying))]
                  {:text ""
                   :graphic
                   (if (fs/file-exists? file-cache dest-path)
                     {:fx/type font-icon/lifecycle
                      :icon-literal "mdi-check:16:white"}
                     {:fx/type :button
                      :text (cond
                              (contains? import-tasks source-path) "queued"
                              copying "copying"
                              :else "")
                      :disable in-progress
                      :tooltip
                      {:fx/type :tooltip
                       :show-delay [10 :ms]
                       :text (str "Copy to " dest-path)}
                      :on-action {:event/type :spring-lobby/add-task
                                  :task
                                  {:spring-lobby/task-type :spring-lobby/import
                                   :importable importable
                                   :spring-isolation-dir spring-isolation-dir}}
                      :graphic
                      {:fx/type font-icon/lifecycle
                       :icon-literal "mdi-content-copy:16:white"}})}))}}]}]}
       {:fx/type :pane})}}))

(defn import-window [state]
  (tufte/profile {:dynamic? true
                  :id :skylobby/ui}
    (tufte/p :import-window
      (import-window-impl state))))
