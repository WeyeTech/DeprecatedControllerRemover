# Controller Cleanup



> [!NOTE]
> An IntelliJ IDEA plugin for automatically detecting and removing deprecated controller methods and unused code.

<!-- Plugin description -->
**Controller Cleanup** is an IntelliJ plugin that intelligently analyzes your Java/Spring codebase to identify and safely remove deprecated controller methods and unused code elements.

This plugin helps maintain clean, efficient codebases by:
- **Detecting deprecated controller methods** that are no longer in use
- **Removing unused final private fields** to reduce memory footprint
- **Cleaning up unused imports** to improve code readability
- **Providing detailed analysis reports** of all cleanup operations
- **Ensuring safe removal** with comprehensive usage analysis

## Key Features

### 🎯 **Controller Cleanup**
- Identifies deprecated methods in Spring controllers with `@Controller` and `@RestController` annotations
- Analyzes usage patterns across the entire project using PSI tree traversal
- Safely removes unused deprecated methods from controllers
- Provides detailed reporting of what was removed

### 🧹 **Code Cleanup Service**
- **Unused Field Removal**: Automatically removes unused final private fields
- **Import Optimization**: Cleans up unused import statements
- **Smart Analysis**: Uses PSI tree traversal for accurate usage detection
- **Safe Operations**: Comprehensive validation before any code removal

### 📊 **Detailed Reporting**
- Real-time progress updates during cleanup operations
- Summary reports showing exactly what was removed
- File-by-file breakdown of cleanup statistics
- User confirmation dialogs for safety

### 🛡️ **Safety First**
- Comprehensive usage analysis before removal
- Respects framework annotations and dependencies
- Preserves code that might be used by reflection or external tools
- Validates method usage across the entire project

## Installation

### From Source (Recommended for Development)
1. Clone the repository: `git clone https://github.com/Ayushsinghal05/controller-cleanup-starter.git`
2. Build the plugin: `./gradlew buildPlugin`
3. Install the generated `.jar` file from `build/distributions/` in IntelliJ IDEA
4. Restart your IDE

### From JetBrains Marketplace (Coming Soon)
1. Open IntelliJ IDEA
2. Go to **Settings/Preferences > Plugins**
3. Search for "Controller Cleanup"
4. Click **Install** and restart your IDE

## Usage

### Quick Cleanup
1. Open your Java/Spring project in IntelliJ IDEA
2. Go to **Tools > Controller Cleanup > Perform Cleanup**
3. Review the analysis results in the dialog
4. Confirm the cleanup operation

### Selective Cleanup
1. Select specific files or directories in the Project view
2. Right-click and choose **Controller Cleanup > Cleanup Selected Files**
3. Review and confirm the changes

### Batch Processing
- The plugin can process entire projects or specific modules
- Supports both individual file and bulk operations
- Provides progress indicators for large codebases
- Handles multiple file types and project structures

## Configuration

The plugin works out-of-the-box with sensible defaults, but you can customize:

- **Analysis scope**: Choose between project-wide or module-specific analysis
- **Safety checks**: Configure which types of code elements to analyze
- **Reporting level**: Set the detail level for cleanup reports
- **Confirmation dialogs**: Enable/disable user confirmation for safety

## Supported Languages & Frameworks

- **Java**: Full support for Java projects with comprehensive PSI analysis
- **Spring Framework**: Specialized analysis for Spring controllers and annotations
- **Maven/Gradle**: Works with both build systems
- **Kotlin**: Basic support (coming in future versions)

## Development

This plugin is built using the IntelliJ Platform SDK and follows best practices for plugin development.

### Prerequisites
- IntelliJ IDEA 2023.1 or later
- Java 17 or later
- Gradle 8.0 or later

### Building from Source

```bash
# Clone the repository
git clone https://github.com/Ayushsinghal05/controller-cleanup-starter.git

# Navigate to the project directory
cd controller-cleanup-starter

# Build the plugin
./gradlew buildPlugin

# Run the plugin in a sandbox IDE
./gradlew runIde

# Run tests
./gradlew test
```

### Project Structure

```
src/main/kotlin/
├── com/github/ayushsinghal05/deprecatedcontrollerremover/
│   ├── services/
│   │   └── MyProjectService.kt            # Project service for sample functionality
│   ├── startup/
│   │   └── MyProjectActivity.kt           # Startup activity for sample functionality
│   └── toolWindow/
│       └── MyToolWindowFactory.kt         # Tool window factory for sample UI
└── com/wheelseye/
    ├── codecleanup/
    │   └── service/
    │       └── CodeCleanupService.kt      # Core cleanup logic for unused imports and fields
    └── controllercleanup/
        ├── action/
        │   └── ControllerCleanupAction.kt # UI actions for controller cleanup
        └── service/
            └── ControllerCleanupService.kt # Controller-specific cleanup logic
```

## How It Works

### Code Analysis Engine

The plugin uses IntelliJ's PSI (Program Structure Interface) to analyze your code:

1. **File Discovery**: Scans your project for Java files using ProjectFileIndex
2. **Usage Analysis**: Traverses the PSI tree to find references using ReferencesSearch
3. **Safety Validation**: Ensures code isn't used by frameworks or reflection
4. **Safe Removal**: Deletes unused code with proper error handling and user confirmation

### Controller Detection

For Spring controllers, the plugin:

- Identifies classes with `@Controller` or `@RestController` annotations using AnnotatedElementsSearch
- Checks for `@Deprecated` annotations on methods within these controllers
- Analyzes usage patterns across the entire project to find unused deprecated methods
- Validates that no active code references the deprecated methods
- Provides detailed reporting before removal

### Field Cleanup

The unused field detection:

- Focuses on `final private` fields for safety
- Excludes fields with annotations (framework usage)
- Uses global search to find all references
- Provides detailed reporting of what was removed

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and add tests
4. Run the test suite: `./gradlew test`
5. Commit your changes: `git commit -m 'Add amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Testing

The plugin includes comprehensive tests:

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*ControllerCleanup*"
./gradlew test --tests "*CodeCleanup*"

# Run with coverage
./gradlew test jacocoTestReport
```

## Troubleshooting

### Common Issues

**Plugin not detecting files**
- Ensure your project is properly indexed
- Check that files are within the project scope
- Verify Java SDK is correctly configured
- Make sure the Java plugin is enabled

**False positives**
- Review the safety settings
- Check for framework annotations
- Verify reflection usage patterns
- Ensure all dependencies are properly resolved

**Performance issues**
- Use module-specific analysis for large projects
- Configure appropriate exclusion patterns
- Consider running cleanup during off-peak hours
- Check available memory and system resources

### Getting Help

If you encounter issues:

1. Check the [GitHub Issues](https://github.com/Ayushsinghal05/DeprecatedControllerRemover/issues) for known problems
2. Review the [Wiki](https://github.com/Ayushsinghal05/DeprecatedControllerRemover/wiki) for detailed guides
3. Join the [Discussions](https://github.com/Ayushsinghal05/DeprecatedControllerRemover/discussions) for community support

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: Report bugs and request features on [GitHub Issues](https://github.com/Ayushsinghal05/controller-cleanup-starter/issues)
- **Discussions**: Join the conversation on [GitHub Discussions](https://github.com/Ayushsinghal05/controller-cleanup-starter/discussions)
- **Documentation**: Check our [Wiki](https://github.com/Ayushsinghal05/controller-cleanup-starter/wiki) for detailed guides

## Roadmap

- [ ] Enhanced Spring controller detection with more annotation types
- [ ] Kotlin language support
- [ ] Custom cleanup rules configuration
- [ ] Integration with CI/CD pipelines
- [ ] Performance optimization for large codebases
- [ ] IDE theme integration
- [ ] Batch processing improvements
- [ ] More detailed reporting and analytics

## Acknowledgments

- Built with the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Inspired by the need for automated code cleanup in large Java/Spring projects
- Community feedback and contributions
- JetBrains for the excellent IntelliJ Platform SDK

---



<!-- Plugin description end -->

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a complete list of changes and version history.

---