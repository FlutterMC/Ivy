# Ivy 🍃

Ivy is a powerful and flexible punishment core for Minecraft servers :3 

## Features
- **Audit Logging**: Keep track of all moderation actions via the Audit log :D
- **Evidence System**: Attach evidence to punishments for better record-keeping :3
- **API**: RESTful API for integration with external tools and services :o

## Requirements

- Java 21 or higher
- Paper 1.21.1 or higher
- Kotlin 2.0.20 or higher

Ivy provides a POWERFUL RESTful API for integration with external tools! 

Endpoints:
- `GET /api/v1/punishments` - List all active punishments
- `POST /api/v1/punishments` - Create a new punishment
- `DELETE /api/v1/punishments` - Remove a punishment
- `GET /api/v1/auditlog` - View the audit log
- `POST /api/v1/evidence` - Add evidence to a punishment
- `POST /api/v1/commands` - Allows to execute cmds

## Building from Source

To build Ivy from source:

1. Clone the repository: `git clone https://github.com/FlutterMC/Ivy.git`
2. Navigate to the project directory: `cd Ivy`
3. Build the project: `./gradlew build`

The built jar will be in `build/libs/Ivy-1.0.jar`.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Mozilla Public License 2.0 - see the [LICENSE](LICENSE) file for details.
Thank you for using Ivy!.
