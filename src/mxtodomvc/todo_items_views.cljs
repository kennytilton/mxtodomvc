(ns mxtodomvc.todo-items-views
  (:require
    [tiltontec.util.core :as util]

    [tiltontec.cell.core :refer-macros [cF cF+ cFn cF+n cFonce] :refer [cI]]

    [tiltontec.model.core
     ; todo trim
     :refer-macros [with-par]
     :refer [matrix mx-par <mget mset!> mswap!> mxu-find-type] :as md]

    [mxweb.gen
     :refer-macros [div section header h1 footer p ul li span button]]
    [mxtodomvc.todo
     :refer [td-completed td-delete!] :as todo]
    [mxtodomvc.todo-view
     :refer [todo-list-item]]
    [cljs.pprint :as pp]))

;; --- convenient accessors ---------------------
;;
;; One point of friction while coding with Matrix is
;; navigating the Matrix to information we need to
;; code up any given functionality.
;;
;; In this case, navigation is by the Matrix pseudo-tyoe,
;; handled by the function 'mxu-find-type'.
;;
;; Matrix includes quite a menagerie of utilities
;; to find other nodes in the Matrix, akin to various
;; ways of authoring CSS selectors.
;;
;; Would a one-atom DB approach work? Not really, for reasons
;; involving the Matrix life-cycle, an advanced topic we would
;; like to defer.

(defn mx-find-matrix [mx]
  (assert mx)
  (mxu-find-type mx ::md/todoApp))

;; Unsurprisingly, the state of the to-dos themselves
;; drives most of the TodoMVC dynamic behavior being exercised.
;; Below we wrap up navigation to that data structure.

(defn mx-todos
  "Given a node in the matrix, navigate to the root and read the todos. After
  the matrix is initially loaded (say in an event handler), one can pass nil
  and find the matrix in @matrix. Put another way, a starting node is required
  during the matrix's initial build."
  ([]
   (<mget @matrix :todos))

  ([mx]
   (if (nil? mx)
     (mx-todos)
     (let [mtrx (mx-find-matrix mx)]
       (assert mtrx)
       (<mget mtrx :todos)))))

(defn mx-todo-items
  ([]
   (mx-todo-items nil))
  ([mx]
   (<mget (mx-todos mx) :items)))

;;; --- views --------------------------------------------------------

(defn todo-items-list []
  (section {:class "main"}
    (ul {:class "todo-list"}
      (for [todo (<mget (mx-todos me) :items)]
        (todo-list-item todo)))))

(defn todo-items-dashboard []
  (footer {:class  "footer"
           :hidden (cF (<mget (mx-todos me) :empty?))}

    (span {:class   "todo-count"
           :content (cF (pp/cl-format nil "<strong>~a</strong>  item~:P remaining"
                          (count (remove td-completed (mx-todo-items me)))))})

    ;;; selector routing coming in the next tag

    (button {:class   "clear-completed"
             :hidden  (cF (empty? (<mget (mx-todos me) :items-completed)))
             :onclick #(doseq [td (filter td-completed (mx-todo-items))]
                         (td-delete! td))}
      "Clear completed")))