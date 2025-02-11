(ns skylobby.fx.sync
  (:require
    [cljfx.ext.node :as fx.ext.node]
    [spring-lobby.fx.font-icon :as font-icon]))


(def ok-green "#008000")
(def warn-yellow "#FFD700")
(def error-red "#DD0000")
(def severity-styles
  {0 {:-fx-base ok-green
      :-fx-background ok-green
      :-fx-background-color ok-green}
   1 {:-fx-base warn-yellow
      :-fx-background warn-yellow
      :-fx-background-color warn-yellow}
   2 {:-fx-base error-red
      :-fx-background error-red
      :-fx-background-color error-red}})

(defn sync-pane
  [{:keys [issues resource]}]
  (let [worst-severity (reduce
                         (fn [worst {:keys [severity]}]
                           (max worst severity))
                         -1
                         issues)]
    {:fx/type :v-box
     :style (merge
              (get severity-styles worst-severity)
              {:-fx-background-radius 3
               :-fx-border-color "#666666"
               :-fx-border-radius 3
               :-fx-border-style "solid"
               :-fx-border-width 1
               :-fx-pref-width 400})
     :children
     (concat
       [{:fx/type :label
         :text (str resource
                    (if (zero? worst-severity) " synced"
                      " status:"))
         :style {:-fx-font-size 16}}]
       (map
         (fn [{:keys [action in-progress human-text severity text tooltip]}]
           (let [font-style {:-fx-font-size 12}
                 display-text (or human-text
                                  (str text " " resource))]
             {:fx/type fx.ext.node/with-tooltip-props
              :props
              (when tooltip
                {:tooltip
                 {:fx/type :tooltip
                  :show-delay [10 :ms]
                  :style {:-fx-font-size 14}
                  :text tooltip}})
              :desc
              (if (or (zero? severity) (not action))
                {:fx/type :label
                 :text display-text
                 :style font-style
                 :graphic
                 {:fx/type font-icon/lifecycle
                  :icon-literal
                  (str "mdi-"
                       (if (zero? severity)
                         "check"
                         (if (= -1 severity)
                           "sync"
                           "exclamation"))
                       ":16:"
                       "black")}}
                (let [style (get severity-styles severity)]
                  {:fx/type :v-box
                   :style (merge style font-style)
                   :children
                   [{:fx/type :button
                     :style (dissoc style :-fx-background-color)
                     :v-box/margin 8
                     :text display-text
                     :disable (boolean in-progress)
                     :on-action action}]}))}))
         issues))}))
