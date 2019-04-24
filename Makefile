.PHONY: test

test:
	clj -A:test:runner

deploy: test
	clj -Spom
	mvn deploy
