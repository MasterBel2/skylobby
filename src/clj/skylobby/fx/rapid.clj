(ns skylobby.fx.rapid
  (:require
    [cljfx.ext.node :as fx.ext.node]
    [clojure.java.io :as io]
    [clojure.string :as string]
    skylobby.fx
    [spring-lobby.fx.font-icon :as font-icon]
    [spring-lobby.rapid :as rapid]
    [spring-lobby.util :as u]
    [taoensso.tufte :as tufte]))


(def rapid-download-window-width 1600)
(def rapid-download-window-height 800)


(def rapid-download-window-keys
  [:css :engine-version :engines :rapid-data-by-hash :rapid-download :rapid-filter :rapid-repo
   :rapid-repos :rapid-packages :rapid-versions :sdp-files :show-rapid-downloader :spring-isolation-dir])

(defn rapid-download-window-impl
  [{:keys [css engine-version engines rapid-download rapid-filter rapid-repo rapid-repos rapid-versions
           rapid-packages screen-bounds sdp-files show-rapid-downloader spring-isolation-dir]}]
  (let [sdp-files (or sdp-files [])
        sdp-hashes (set (map rapid/sdp-hash sdp-files))
        sorted-engine-versions (->> engines
                                    (map :engine-version)
                                    sort)
        filtered-rapid-versions (->> rapid-versions
                                     (filter
                                       (fn [{:keys [id]}]
                                         (if rapid-repo
                                           (string/starts-with? id (str rapid-repo ":"))
                                           true)))
                                     (filter
                                       (fn [{:keys [version] :as i}]
                                         (if-not (string/blank? rapid-filter)
                                           (or
                                             (string/includes?
                                               (string/lower-case version)
                                               (string/lower-case rapid-filter))
                                             (string/includes?
                                               (:hash i)
                                               (string/lower-case rapid-filter)))
                                           true))))
        engines-by-version (into {} (map (juxt :engine-version identity) engines))
        engine-file (:file (get engines-by-version engine-version))
        {:keys [width height]} screen-bounds]
    {:fx/type :stage
     :showing (boolean show-rapid-downloader)
     :title (str u/app-name " Rapid Downloader")
     :icons skylobby.fx/icons
     :on-close-request {:event/type :spring-lobby/dissoc
                        :key :show-rapid-downloader}
     :width ((fnil min rapid-download-window-width) width rapid-download-window-width)
     :height ((fnil min rapid-download-window-height) height rapid-download-window-height)
     :scene
     {:fx/type :scene
      :stylesheets (skylobby.fx/stylesheet-urls css)
      :root
      (if show-rapid-downloader
        {:fx/type :v-box
         :children
         [{:fx/type :h-box
           :style {:-fx-font-size 16}
           :alignment :center-left
           :children
           [{:fx/type :label
             :text " Engine for pr-downloader: "}
            {:fx/type :combo-box
             :value (str engine-version)
             :items (or (seq sorted-engine-versions)
                        [])
             :on-value-changed {:event/type :spring-lobby/version-change}}
            {:fx/type :button
             :text " Refresh "
             :on-action {:event/type :spring-lobby/add-task
                         :task {:spring-lobby/task-type :spring-lobby/update-rapid
                                :force true}}
             :graphic
             {:fx/type font-icon/lifecycle
              :icon-literal "mdi-refresh:16:white"}}]}
          {:fx/type :h-box
           :style {:-fx-font-size 16}
           :alignment :center-left
           :children
           (concat
             [{:fx/type :label
               :text " Filter Repo: "}
              {:fx/type :combo-box
               :value (str rapid-repo)
               :items (or (seq rapid-repos)
                          [])
               :on-value-changed {:event/type :spring-lobby/rapid-repo-change}}]
             (when rapid-repo
               [{:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Clear rapid repo filter"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/dissoc
                              :key :rapid-repo}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-close:16:white"}}}])
             [{:fx/type :label
               :text " Rapid Filter: "}
              {:fx/type :text-field
               :text rapid-filter
               :prompt-text "Filter by name or path"
               :on-text-changed {:event/type :spring-lobby/assoc
                                 :key :rapid-filter}}]
             (when-not (string/blank? rapid-filter)
               [{:fx/type fx.ext.node/with-tooltip-props
                 :props
                 {:tooltip
                  {:fx/type :tooltip
                   :show-delay [10 :ms]
                   :text "Clear filter"}}
                 :desc
                 {:fx/type :button
                  :on-action {:event/type :spring-lobby/dissoc
                              :key :rapid-filter}
                  :graphic
                  {:fx/type font-icon/lifecycle
                   :icon-literal "mdi-close:16:white"}}}]))}
          {:fx/type :table-view
           :column-resize-policy :constrained ; TODO auto resize
           :items (or (seq filtered-rapid-versions)
                      [])
           :style {:-fx-font-size 16}
           :columns
           [{:fx/type :table-column
             :sortable false
             :text "ID"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [i]
                {:text (str (:id i))})}}
            {:fx/type :table-column
             :sortable false
             :text "Hash"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [i]
                {:text (str (:hash i))})}}
            {:fx/type :table-column
             :text "Version"
             :cell-value-factory :version
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [version]
                {:text (str version)})}}
            {:fx/type :table-column
             :text "Download"
             :sortable false
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [i]
                (let [download (get rapid-download (:id i))]
                  (merge
                    {:text (str (:message download))
                     :style {:-fx-font-family skylobby.fx/monospace-font-family}}
                    (cond
                      (sdp-hashes (:hash i))
                      {:graphic
                       {:fx/type font-icon/lifecycle
                        :icon-literal "mdi-check:16:white"}}
                      (:running download)
                      nil
                      (not engine-file)
                      {:text "Needs an engine"}
                      :else
                      {:graphic
                       {:fx/type :button
                        :on-action {:event/type :spring-lobby/add-task
                                    :task
                                    {:spring-lobby/task-type :spring-lobby/rapid-download
                                     :engine-file engine-file
                                     :rapid-id (:id i)
                                     :spring-isolation-dir spring-isolation-dir}}
                        :graphic
                        {:fx/type font-icon/lifecycle
                         :icon-literal "mdi-download:16:white"}}}))))}}]}
          {:fx/type :h-box
           :alignment :center-left
           :style {:-fx-font-size 16}
           :children
           [{:fx/type :label
             :text " Packages"}
            {:fx/type fx.ext.node/with-tooltip-props
             :props
             {:tooltip
              {:fx/type :tooltip
               :show-delay [10 :ms]
               :text "Open rapid packages directory"}}
             :desc
             {:fx/type :button
              :on-action {:event/type :spring-lobby/desktop-browse-dir
                          :file (io/file spring-isolation-dir "packages")}
              :graphic
              {:fx/type font-icon/lifecycle
               :icon-literal "mdi-folder:16:white"}}}]}
          {:fx/type :table-view
           :column-resize-policy :constrained ; TODO auto resize
           :items (or (seq rapid-packages)
                      [])
           :style {:-fx-font-size 16}
           :columns
           [{:fx/type :table-column
             :text "Filename"
             :sortable false
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [i] {:text (:filename i)})}}
            {:fx/type :table-column
             :sortable false
             :text "ID"
             :cell-value-factory identity
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [i] {:text (:id i)})}}
            {:fx/type :table-column
             :text "Version"
             :cell-value-factory :version
             :cell-factory
             {:fx/cell-type :table-cell
              :describe
              (fn [version] {:text (str version)})}}]}]}
       {:fx/type :pane})}}))

(defn rapid-download-window [state]
  (tufte/profile {:dynamic? true
                  :id :skylobby/ui}
    (tufte/p :rapid-download-window
      (rapid-download-window-impl state))))
