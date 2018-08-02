(ns mxtodomvc.build
  (:require
    [tiltontec.util.core :as util]

    [tiltontec.cell.core :refer-macros [cF cF+ cFn cF+n cFonce cF1] :refer [cI]]

    [tiltontec.model.core
     ; todo trim
     :refer-macros [with-par]
     :refer [matrix mx-par <mget mset!> mswap!>
             fget mxi-find mxu-find-type
             kid-values-kids] :as md]
    [mxweb.gen
     :refer-macros [div section header h1 footer p ul li span]]
    [mxtodomvc.todo
     :refer [make-todo td-title] :as todo]
    [mxtodomvc.web-components :as webco]
    [mxtodomvc.todo-view
     :refer [todo-list-item]]))

;; New for this tag, we have moved the wall-clock to a reusable
;; "web-components" namespace and converted the hard-coded credits
;; to be a reusable component, and placing it in the same home.

(def mxtodo-credits
  ["Double-click a to-do list item to edit it."
   "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
   "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."])

(defn matrix-build []
  (reset! md/matrix
    ;; now we provide an optional "type" to support Matrix node space search
    (md/make ::todoApp
      ;;
      ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
      ;;
      :todos (todo/todo-list ["Wash car"
                               "Walk dog"
                               "Do laundry"
                               "Mow lawn"])
      :mx-dom (cFonce
                (with-par me
                  (section {:class "todoapp" :style "padding:24px"}
                    (webco/wall-clock :date 60000 0 15)
                    (webco/wall-clock :time 1000 0 8)
                    (header {:class "header"}
                      (h1 "todos")
                      (section {:class "main"}
                        (ul {:class "todo-list"}
                          ;; below we have 'mxu-find-type'.
                          ;; we have quite a menagerie of utilities
                          ;; to find other nodes in the Matrix. This
                          ;; is akin to CSS selectors, and perhaps the
                          ;; chief source of friction while coding. But
                          ;; we have considered the single-atom DB and
                          ;; think it artificially collects state in
                          ;; one structure. That sounds clean, but we
                          ;; do not like artificial. It might also
                          ;; complicate the Matrix, but we will happily
                          ;; offer support to someone caring to try
                          ;; the one-atom DB trick. It does have the appeal
                          ;; of making it simpler to reference state.
                          (let [matrix (mxu-find-type me ::todoApp)
                                todo-list (<mget matrix :todos)]
                            (doall (for [todo (<mget todo-list :items)]
                                      (todo-list-item todo))))))
                      (webco/app-credits mxtodo-credits))))))))
