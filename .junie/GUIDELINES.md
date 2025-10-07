When writing tests, ALWAYS include debug information printed to stdout which is descriptive of what the test is doing. This is crucial for understanding test failures and for debugging purposes.

Whenever a test touches the filesystem or openGL context, ensure that the test framework we created is used appropriately. If nothing is renderered, be sure to use the leadless mode.

Include writing tests as a default part of your development process. Tests should be devloped concurrently with the features they are meant to validate.

When writing tests, consider edge cases and potential failure points. Aim for comprehensive coverage to ensure robustness.



You MUST judge each change analytically. Think of enhancements that are in line with the intent described. Additionally, if there are things that should be removed for a fluent API design, do so. Finally, think of completely original enhancements not mentioned by me. Be highly imaginative and implement every idea which seems like a coherent addition. Consider features of similar projects with adjacent design philosophies & intents. Reference and implement their ideas as you see fit.

When implementing new features, always consider the overall design philosophy and intent of the project. Ensure that new features align with these principles and enhance the user experience.

Whewn writing the code for new features, do not put multiple classes or interfaces or annotions in a single file. Each class, interface, or annotation should be in its own file named after it.

When writing code, always prioritize readability and maintainability. Use clear and descriptive names for variables, functions, classes, and other identifiers. Write comments where necessary to explain complex logic or decisions.