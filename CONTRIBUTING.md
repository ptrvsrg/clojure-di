# Contributing Guidelines

Thank you for considering contributing to our project!

## Development Environment

1. Install [Leiningen](https://leiningen.org/)
2. Clone the repository
3. Install dependencies: `lein deps`

## Code Style

- Follow Clojure's official style guide
- Use meaningful function and variable names
- Prefer immutable data structures
- Write descriptive function docstrings
- Use `->` and `->>` threading macros for readability

## Testing

- Write tests for all new functionality
- Use `clojure.test` for testing
- Follow the pattern: `function-name-test`
- Cover edge cases

## Pull Request Process

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Run the test suite: `lein test`
6. Update documentation if necessary
7. Submit a pull request

## Code Review

- Ensure code follows Clojure conventions
- Check for comprehensive test coverage
- Verify documentation is up to date
- Confirm all tests pass
