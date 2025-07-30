# Deprecated Controller Remover

> [!NOTE]
> An IntelliJ IDEA plugin for automatically detecting and removing deprecated controller methods and cleaning up unused code.

<!-- Plugin description -->
**Deprecated Controller Remover** is an IntelliJ plugin that intelligently analyzes your Java/Spring codebase to identify and safely remove deprecated controller methods and unused code elements.

This plugin helps maintain clean, efficient codebases by:
- **Detecting deprecated controller methods** that are no longer in use
- **Removing unused final private fields** to reduce memory footprint
- **Cleaning up unused imports** to improve code readability
- **Removing empty classes** to streamline code structure
- **Providing detailed analysis reports** of all cleanup operations
- **Ensuring safe removal** with comprehensive usage analysis

## Key Features

### 🎯 **Deprecated Controller Remover**
- Identifies deprecated methods in Spring controllers with `@Controller` and `@RestController` annotations
- Analyzes usage patterns across the entire project using PSI tree traversal
- Safely removes unused deprecated methods from controllers
- Provides detailed reporting of what was removed
- **Tool Window**: Dedicated console interface for monitoring deprecated controller removal operations

### 🧹 **Code Cleanup Service**
- **Unused Field Removal**: Automatically removes unused final private fields
- **Import Optimization**: Cleans up unused import statements
- **Empty Class Removal**: Removes classes with no methods, fields, or nested classes
- **Smart Analysis**: Uses PSI tree traversal for accurate usage detection
- **Safe Operations**: Comprehensive validation before any code removal
- **File Marking**: Works with files marked with `//Controller Cleaner` comment
- **Tool Window**: Dedicated console interface for monitoring code cleanup operations

### 📊 **Dual Tool Windows**
- **Deprecated Controller Remover Tool Window**: Black console-style interface for controller-specific operations
- **Code Cleanup Tool Window**: Black console-style interface for general code cleanup operations
- **Real-time Progress**: Live updates during cleanup operations
- **Detailed Logging**: Comprehensive logging of all operations and results
- **Side-by-side Operation**: Both tool windows can be used simultaneously

### 🛡️ **Safety First**
- Comprehensive usage analysis before removal
- Respects framework annotations and dependencies
- Preserves code that might be used by reflection or external tools
- Validates method usage across the entire project
- Multiple cleanup passes to catch newly unused elements
- User confirmation dialogs for all operations

## Installation

### From Source (Recommended for Development)
1. Clone the repository: `git clone https://github.com/WeyeTech/DeprecatedControllerRemover.git`
2. Build the plugin: `./gradlew buildPlugin`
3. Install the generated `.jar` file from `build/distributions/` in IntelliJ IDEA
4. Restart your IDE

### From JetBrains Marketplace (Coming Soon)
1. Open IntelliJ IDEA
2. Go to **Settings/Preferences > Plugins**
3. Search for "Deprecated Controller Remover"
4. Click **Install** and restart your IDE

## Usage

### Deprecated Controller Remover
1. Open your Java/Spring project in IntelliJ IDEA
2. Go to **Tools > Deprecated Controller Remover**
3. The **Deprecated Controller Remover** tool window will appear at the bottom with a black console interface
4. Review the analysis results in the dialog and console output
5. Confirm the cleanup operation

### Code Cleanup
1. Mark files for cleanup by adding `//Controller Cleaner` comment at the top of Java files
2. Go to **Tools > Clean Marked Files**
3. The **Code Cleanup** tool window will appear at the bottom with a black console interface
4. Review the analysis results showing unused imports, fields, and empty classes
5. Confirm the cleanup operation

### Tool Windows
- Both tool windows appear as buttons in the bottom tool window area
- Click on either button to open the respective console interface
- Both windows provide real-time logging and progress updates
- Operations can be run independently or simultaneously

## Configuration

The plugin works out-of-the-box with sensible defaults:

- **File Marking**: Add `//Controller Cleaner` to Java files you want to clean up
- **Analysis Scope**: Automatically analyzes the entire project
- **Safety Checks**: Comprehensive validation before removal
- **Multiple Passes**: Up to 3 cleanup passes to catch newly unused elements
- **Progress Reporting**: Detailed progress indicators and logging

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
git clone https://github.com/WeyeTech/DeprecatedControllerRemover.git

# Navigate to the project directory
cd DeprecatedControllerRemover

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
└── com/wheelseye/
    ├── codecleanup/
    │   ├── action/
    │   │   └── CodeCleanupAction.kt            # Code cleanup UI actions
    │   ├── service/
    │   │   └── CodeCleanupService.kt           # Code cleanup logic (imports, fields, empty classes)
    │   └── toolWindow/
    │       ├── CodeCleanupToolWindowFactory.kt # Code cleanup tool window factory
    │       ├── CodeCleanupToolWindowPanel.kt   # Code cleanup tool window UI
    │       └── CodeCleanupToolWindowService.kt # Code cleanup tool window service
    └── deprecatedcontrollerremover/
        ├── action/
        │   └── DeprecatedControllerRemoverAction.kt      # Deprecated controller remover UI actions
        ├── service/
        │   └── DeprecatedControllerRemoverService.kt     # Deprecated controller remover logic (deprecated methods)
        └── toolWindow/
            ├── DeprecatedControllerRemoverToolWindowFactory.kt # Deprecated controller remover tool window factory
            ├── DeprecatedControllerRemoverToolWindowPanel.kt   # Deprecated controller remover tool window UI
            └── DeprecatedControllerRemoverToolWindowService.kt # Deprecated controller remover tool window service
```

## How It Works

### Deprecated Controller Remover Engine

The deprecated controller remover uses IntelliJ's PSI (Program Structure Interface) to analyze Spring controllers:

1. **Controller Detection**: Identifies classes with `@Controller` or `@RestController` annotations
2. **Deprecated Method Analysis**: Finds methods marked with `@Deprecated` annotation
3. **Usage Analysis**: Traverses the PSI tree to find references using ReferencesSearch
4. **Safety Validation**: Ensures deprecated methods aren't used by active code
5. **Safe Removal**: Deletes unused deprecated methods with user confirmation

### Code Cleanup Engine

The code cleanup analyzes files marked with `//Controller Cleaner`:

1. **File Discovery**: Scans for Java files with the cleanup marker
2. **Import Analysis**: Identifies unused import statements (excluding java.lang.*)
3. **Field Analysis**: Finds unused final private fields (excluding annotated fields)
4. **Class Analysis**: Identifies empty classes with no methods, fields, or nested classes
5. **Multi-pass Cleanup**: Runs up to 3 passes to catch newly unused elements
6. **Safe Removal**: Deletes unused elements with comprehensive error handling

### Tool Window System

Both tool windows provide:
- **Console Interface**: Black background with white text for clear visibility
- **Real-time Logging**: Live updates during operations
- **Progress Tracking**: Detailed progress indicators
- **Error Reporting**: Comprehensive error messages and handling

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
./gradlew test --tests "*DeprecatedControllerRemover*"
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

**Code cleanup not working**
- Ensure files are marked with `//Controller Cleaner` comment
- Check that the comment is at the top of the Java file
- Verify the file extension is `.java`

**False positives**
- Review the safety settings
- Check for framework annotations
- Verify reflection usage patterns
- Ensure all dependencies are properly resolved

**Tool windows not appearing**
- Check that the plugin is properly installed
- Restart the IDE after installation
- Verify the tool window buttons are visible in the bottom area

### Getting Help

If you encounter issues:

1. Check the [GitHub Issues](https://github.com/WeyeTech/DeprecatedControllerRemover/issues) for known problems
2. Review the [Wiki](https://github.com/WeyeTech/DeprecatedControllerRemover/wiki) for detailed guides
3. Join the [Discussions](https://github.com/WeyeTech/DeprecatedControllerRemover/discussions) for community support

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: Report bugs and request features on [GitHub Issues](https://github.com/WeyeTech/DeprecatedControllerRemover/issues)
- **Discussions**: Join the conversation on [GitHub Discussions](https://github.com/WeyeTech/DeprecatedControllerRemover/discussions)
- **Documentation**: Check our [Wiki](https://github.com/WeyeTech/DeprecatedControllerRemover/wiki) for detailed guides

## Roadmap

- [ ] Enhanced Spring controller detection with more annotation types
- [ ] Kotlin language support
- [ ] Custom cleanup rules configuration
- [ ] Integration with CI/CD pipelines
- [ ] Performance optimization for large codebases
- [ ] IDE theme integration
- [ ] Batch processing improvements
- [ ] More detailed reporting and analytics
- [ ] Custom file marking patterns
- [ ] Export cleanup reports

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