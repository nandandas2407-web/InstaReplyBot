# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in InstaReply Bot, please report it responsibly:

1. **Do NOT** open a public GitHub issue for security vulnerabilities
2. Email the maintainer directly or use GitHub's private vulnerability reporting
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Response Time

- Acknowledgment within 48 hours
- Assessment within 1 week
- Patch for confirmed vulnerabilities as soon as possible

## Security Considerations

### API Keys
- All API keys are stored locally on device using Android's DataStore
- Keys are never transmitted to any server other than the configured AI provider
- The app never requests internet permission beyond what's needed for API calls

### Instagram Integration
- **No Instagram login required** — the app uses Android's Notification Listener API
- No Instagram credentials are stored or transmitted
- No screen scraping of Instagram content

### Data Privacy
- All data (rules, contacts, reply logs) stays on your device
- No analytics or tracking
- No data collection or transmission to third parties

## Best Practices for Users

1. Only install APKs from official GitHub releases
2. Keep your device and app updated
3. Review the permissions you grant
4. Use free API tiers to minimize exposure
5. Regularly check for updates

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest release | Yes |
| Older versions | No |

Always use the latest release from the [Releases page](https://github.com/nandandas2407-web/InstaReplyBot/releases/latest).
