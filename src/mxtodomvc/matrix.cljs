(ns mxtodomvc.matrix
  (:require
    [tiltontec.util.core :as util]

    [tiltontec.cell.core :refer-macros [cF cF+ cFn cF+n cFonce cF1] :refer [cI]]

    [tiltontec.model.core
     ; todo trim
     :refer-macros [with-par]
     :refer [matrix mx-par <mget mset!> mswap!> mxu-find-type] :as md]
    [mxweb.gen
     :refer-macros [div section header h1 footer p ul
                    li span input label]]

    [mxtodomvc.todo
     :refer [make-todo td-title td-completed] :as todo]
    [mxtodomvc.web-components :as webco]
    [mxtodomvc.todo-items-views
     :refer [mx-todo-items
             todo-items-list
             todo-items-dashboard]]
    [mxtodomvc.todo-view
     :refer [todo-list-item]]

    [bide.core :as r]
    [clojure.string :as str]
    [goog.events.Event :as event]
    [goog.dom.forms :as form]))

;; New for this tag, we have moved the wall-clock to a reusable
;; "web-components" namespace and converted the hard-coded credits
;; to be a reusable component, and placing it in the same home.

(def mxtodo-credits
  ["Double-click a to-do list item to edit it."
   "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
   "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."])

(defn todo-entry-field []
  (input {:class       "new-todo"
          :autofocus   true
          :placeholder "What needs doing?"
          :onkeypress  #(when (= (.-key %) "Enter")
                          (let [raw (form/getValue (.-target %))
                                title (str/trim raw)]
                            (when-not (str/blank? title)
                              (mswap!> (<mget @matrix :todos) :items-raw conj
                                (make-todo title)))
                            (form/setValue (.-target %) "")))}))

(defn matrix-build! []
  (reset! md/matrix
    ;; now we provide an optional "type" to support Matrix node space search
    (md/make ::md/todoApp
      ;;
      ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
      ;;
      :route (cI "All")
      :route-starter (r/start! (r/router [["/" :All]
                                          ["/active" :Active]
                                          ["/completed" :Completed]])
                       {:default     :ignore
                        :on-navigate (fn [route params query]
                                       (when-let [mtx @md/matrix]
                                         (mset!> mtx :route (name route))))})

      :todos (todo/todo-list ["Yankees"])

      :mx-dom (cFonce
                (with-par me
                  (section {:class "todoapp" :style "padding:24px"}
                    ;(webco/wall-clock :date 60000 0 15)
                    ;(webco/wall-clock :time 1000 0 8)
                    (header {:class "header"}
                      (h1 "todos?")
                      (todo-entry-field))
                    (todo-items-list)
                    (todo-items-dashboard)
                    (webco/app-credits mxtodo-credits)))))))

