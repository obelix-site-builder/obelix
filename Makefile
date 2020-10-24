SOURCES := $(shell find src -name '*.cljs')

.PHONY: clean publish

out: build.edn $(SOURCES)
	clojure -m cljs.main -co build.edn -c
	touch out

publish: package.json out
	npm publish

clean:
	rm -rf out
