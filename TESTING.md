# Testing Guidelines for jDriveClonr

This document outlines the testing approach for jDriveClonr and provides guidance for contributors on how to test their changes.

## Testing Philosophy

jDriveClonr follows these testing principles:
- All new features should include corresponding tests
- Bug fixes should include tests that verify the fix
- Critical paths (authentication, file download, export) should have comprehensive test coverage
- Performance-sensitive areas should include performance tests

## Test Categories

### Unit Tests

Unit tests should be written for all non-UI components including:
- Service classes
- Model classes
- Utility functions

Unit tests should be placed in `src/test/java/com/ibrasoft/jdriveclonr/` mirroring the main source structure.

### Integration Tests

Integration tests should verify that different components work together correctly:
- Google API integration
- File system operations
- Configuration persistence

### UI Tests

UI tests should verify that the user interface behaves as expected:
- Component layout and rendering
- User interactions
- Error handling and feedback

## Running Tests

To run the test suite:

```bash
mvn test
```

To run specific test categories:

```bash
mvn test -Dtest=*ServiceTest  # Run all service tests
mvn test -Dtest=DriveAPIServiceTest  # Run a specific test class
```

## Test Mocking

When testing components that interact with external services (Google Drive API, file system):
- Use mocking frameworks like Mockito
- Create test fixtures that simulate realistic data
- Test both success and failure scenarios

## Testing with Google Drive API

When testing against the actual Google Drive API:
- Use test accounts with limited data
- Never run tests against production accounts
- Follow test account credential rotation practices
- Respect Google API rate limits

## Performance Testing

For performance-critical operations:
- Measure throughput and latency
- Test with various thread counts (1-10)
- Test with different file sizes and types
- Include benchmarks for comparison

## Test Reports

After running tests, review the generated reports in:
- `target/surefire-reports/` for test results
- `target/site/jacoco/` for code coverage (if JaCoCo is configured)

## Continuous Integration

All tests are run automatically on pull requests. Ensure your changes pass all tests before submitting a PR.

## Adding New Tests

When adding new features or fixing bugs:
1. Create test cases that verify the functionality
2. Include both positive and negative test cases
3. Consider edge cases and exception handling
4. Document any test data or setup requirements

## Code Coverage Goals

The project aims to maintain:
- At least 80% line coverage for service classes
- At least 70% line coverage for model classes
- At least 60% overall code coverage

## Testing Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Google Drive API Testing Guide](https://developers.google.com/drive/api/v3/test)