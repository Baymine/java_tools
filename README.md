# Enhanced SQL Terminal

An interactive SQL terminal with advanced features including command history, line editing, and pager support.

## Features

- Command history with persistent storage
- Line editing with Emacs-style shortcuts
- Pager support for query results
- Multi-line SQL command support
- Customizable output formatting
- Error handling and logging
- Support for various SQL databases

## Prerequisites

- Java 9 or higher
- Maven 3.6 or higher
- A supported SQL database (MySQL, PostgreSQL, etc.)
- `less` command available in system path (for pager functionality)

## Building from Source

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd java_tools
   ```

2. Build with Maven:
   ```bash
   mvn clean package
   ```

## Running the Application

1. Ensure your database is running and accessible

2. Run the application:
   ```bash
   java -jar target/java_tools-1.0-SNAPSHOT.jar
   ```

## Terminal Commands

### Basic Commands
- `help` - Display help information
- `exit` - Exit the terminal
- `clear` - Clear the screen
- `history` - Show command history

### Pager Commands
- `pager less -S` - Set pager with horizontal scroll
- `pager <command>` - Set custom pager command
- `nopager` or `\n` - Disable pager
- `\p` - Show current pager setting

### SQL Commands
- Enter SQL commands ending with semicolon (;)
- Multi-line commands are supported
- Results are automatically paged for large outputs

### Keyboard Shortcuts
- Up/Down - Navigate through command history
- Left/Right - Move cursor within line
- Ctrl+Left/Right - Move cursor by word
- Home/End - Move to start/end of line
- Ctrl+C - Cancel current input
- Ctrl+D - Exit the terminal
- Ctrl+R - Search command history

## Configuration

### Logging
- Logs are stored in `logs/sql-terminal.log`
- Log files are rotated daily
- Last 7 days of logs are retained
- Error messages are also shown in console

### History
- Command history is stored in `~/.sql_history`
- History size is limited to 1000 entries
- History is persisted between sessions

## Troubleshooting

### Common Issues

1. Terminal not displaying properly:
   - Ensure your terminal supports ANSI escape sequences
   - Try running with `TERM=xterm-256color`

2. Pager not working:
   - Verify `less` is installed and in system PATH
   - Try setting a different pager command
   - Use `\p` to check current pager settings

3. Database connection issues:
   - Verify database is running and accessible
   - Check connection parameters
   - Review logs in `logs/sql-terminal.log`

### Debug Mode

For more detailed logging, modify `src/main/resources/logback.xml`:
- Set root level to "DEBUG" for more verbose logging
- Add additional loggers for specific components

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
