# BDD Portability Rules

The shared feature files must run across Cucumber JVM, CucumberJS, and pytest-bdd.

## Use initially

```text
Feature
Rule
Background
Scenario
Scenario Outline
Examples
Given / When / Then / And
Tags
Simple parameters
```

## Use carefully later

```text
Data tables
Doc strings
Custom parameter types
Complex hooks
```

## Avoid initially

```text
Runner-specific syntax
Language-specific step tricks
Complex custom transformers
Formatter-specific behavior
```

## Step wording rules

- Use domain language.
- Keep technical details out of Gherkin.
- Avoid near-duplicate steps.
- Put HTTP calls, waits, retries, parsing, and evidence writing in support classes.
