(ns skylobby.fx.uikeys
  (:require
    [cljfx.ext.table-view :as fx.ext.table-view]
    [clojure.string :as string]
    clojure.set
    [skylobby.fx :as skylobby.fx]
    [spring-lobby.spring.uikeys :as uikeys]
    [spring-lobby.util :as u]
    [taoensso.timbre :as log])
  (:import
    (javafx.scene.input KeyCode KeyEvent)))


(def bind-keycodes
  {"CTRL" KeyCode/CONTROL
   "ESC" KeyCode/ESCAPE
   "BACKSPACE" KeyCode/BACK_SPACE
   "." KeyCode/PERIOD
   "," KeyCode/COMMA
   "+" KeyCode/PLUS
   "-" KeyCode/MINUS
   "=" KeyCode/EQUALS
   "_" KeyCode/UNDERSCORE
   ";" KeyCode/SEMICOLON
   "^" KeyCode/CIRCUMFLEX
   "`" KeyCode/BACK_QUOTE
   "[" KeyCode/OPEN_BRACKET
   "]" KeyCode/CLOSE_BRACKET
   "/" KeyCode/SLASH
   "\\" KeyCode/BACK_SLASH
   "PAGEUP" KeyCode/PAGE_UP
   "PAGEDOWN" KeyCode/PAGE_DOWN})


(defn- bind-key-to-javafx-keycode [bind-key-piece]
  (or (KeyCode/getKeyCode bind-key-piece)
      (get bind-keycodes bind-key-piece)
      (try (KeyCode/valueOf bind-key-piece)
           (catch Exception e
             (log/trace e "Error getting KeyCode for" bind-key-piece)))))

(def keycode-binds
  (clojure.set/map-invert bind-keycodes))


(defn key-event-to-uikeys-bind [^KeyEvent key-event]
  (str
    (when (.isAltDown key-event)
      "alt,")
    (when (.isControlDown key-event)
      "ctrl,")
    (when (.isShiftDown key-event)
      "shift,")
    (or (get keycode-binds (.getCode key-event))
        (.getText key-event))))


(defn uikeys-window [{:keys [css filter-uikeys-action selected-uikeys-action show-uikeys-window uikeys]}]
  (let [default-uikeys (or (u/try-log "parse uikeys" (uikeys/parse-uikeys))
                           [])
        filtered-uikeys (->>  default-uikeys
                              (filter
                                (fn [{:keys [bind-action]}]
                                  (if (string/blank? filter-uikeys-action)
                                    true
                                    (string/includes?
                                      (string/lower-case bind-action)
                                      (string/lower-case filter-uikeys-action)))))
                              (sort-by :bind-key))
        uikeys-overrides (or uikeys {})]
    {:fx/type :stage
     :showing (boolean show-uikeys-window)
     :title (str u/app-name " UI Keys Editor")
     :icons skylobby.fx/icons
     :on-close-request {:event/type :spring-lobby/assoc
                        :key :show-uikeys-window}
     :width 1200
     :height 1000
     :scene
     {:fx/type :scene
      :stylesheets (skylobby.fx/stylesheet-urls css)
      :root
      (if show-uikeys-window
        {:fx/type :v-box
         :style {:-fx-font-size 14}
         :children
         (if show-uikeys-window
           [{:fx/type :h-box
             :alignment :center-left
             :children
             [{:fx/type :label
               :text " Filter action: "}
              {:fx/type :text-field
               :text (str filter-uikeys-action)
               :prompt-text "filter"
               :on-text-changed {:event/type ::assoc
                                 :key :filter-uikeys-action}}]}
            {:fx/type fx.ext.table-view/with-selection-props
             :v-box/vgrow :always
             :props
             {:selection-mode :single
              :on-selected-item-changed
              {:event/type ::uikeys-select}}
             :desc
             {:fx/type :table-view
              :column-resize-policy :constrained
              :items (or (seq filtered-uikeys) [])
              :on-key-pressed {:event/type ::uikeys-pressed
                               :selected-uikeys-action selected-uikeys-action}
              :columns
              [
               {:fx/type :table-column
                :text "Action"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [i]
                   {:text (str (:bind-action i))})}}
               {:fx/type :table-column
                :text "Bind"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [i]
                   {:text (pr-str (:bind-key i))})}}
               {:fx/type :table-column
                :text "Parsed"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [i]
                   {:text (pr-str (uikeys/parse-bind-keys (:bind-key i)))})}}
               {:fx/type :table-column
                :text "JavaFX KeyCode"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [i]
                   (let [bind-key-uc (string/upper-case (:bind-key i))
                         parsed (uikeys/parse-bind-keys bind-key-uc)
                         key-codes (map
                                     (partial map (comp #(when % (str %)) bind-key-to-javafx-keycode))
                                     parsed)]
                     {:text (pr-str key-codes)}))}}
               {:fx/type :table-column
                :text "Comment"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [{:keys [bind-comment]}]
                   (merge
                     {:text (str bind-comment)}
                     (when bind-comment
                       {:tooltip
                        {:fx/type :tooltip
                         :show-delay [10 :ms]
                         :style {:-fx-font-size 15}
                         :text (str bind-comment)}})))}}
               {:fx/type :table-column
                :text "Override"
                :cell-value-factory identity
                :cell-factory
                {:fx/cell-type :table-cell
                 :describe
                 (fn [i]
                   {:text (pr-str (get uikeys-overrides (:bind-action i)))})}}]}}]
           {:fx/type :label
            :text "window hidden"})}
        {:fx/type :pane})}}))
