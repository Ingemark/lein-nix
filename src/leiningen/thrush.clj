(ns leiningen.thrush
  (:require [leiningen.core.main :refer [resolve-and-apply]]
            [leiningen.do :refer [group-args]]
            [lein-nix.core :refer [prj-result]]))

(defn ^:no-project-needed ^:higher-order thrush
  "Like ->, but for leiningen tasks.

Each comma-separated group should be a task name followed by optional arguments."
  [project & args]
  ^:boxed-result
  (zipmap [:project :result]
          (reduce (fn [[project & r] [task-name & args]]
                    (->> (resolve-and-apply project (concat [task-name] r args))
                         (prj-result project)))
                  [project] (group-args args))))
