.PHONY: test

test:
	clj -A:test:runner

deploy: test
	clj -Spom
	mvn deploy

push:
	mvn deploy
