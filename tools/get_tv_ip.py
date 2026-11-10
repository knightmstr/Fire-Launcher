#!/usr/bin/env python3
"""
tools/get_tv_ip.py
CLI helper to prompt for, validate, and emit a valid Amazon Fire TV IPv4 address.
Formats the final successful IP as 'SUCCESS:<IP_ADDRESS>' for programmatic agent consumption.
"""

import sys
import re

# Robust IPv4 regex: ensures 4 octets, each strictly in range 0-255, no leading zero misuse
IPV4_REGEX = re.compile(
    r"^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.){3}"
    r"(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
)

BANNER = r"""
========================================================================
            🔥 Fire TV Wireless IP Discovery & Configuration 🔥
========================================================================
To locate your Fire TV's local IP address on your television screen:

  1. Turn on your Amazon Fire TV.
  2. Navigate to:
       Settings  ->  My Fire TV (or 'Device & Software')  ->  About
  3. Select 'Network' from the menu on the right.
  4. Look for the 'IP Address' field on the network status card.
       (Example: 192.168.1.45 or 10.0.0.88)
========================================================================
"""


def is_valid_ipv4(ip_str: str) -> bool:
    """Validate string against strict IPv4 pattern with 0-255 octet bounds."""
    if not ip_str or not isinstance(ip_str, str):
        return False
    trimmed = ip_str.strip()
    if not IPV4_REGEX.match(trimmed):
        return False
    
    # Double-check octet integer boundaries and reject reserved loopback/broadcast if needed
    try:
        octets = [int(part) for part in trimmed.split(".")]
        if len(octets) != 4:
            return False
        for octet in octets:
            if octet < 0 or octet > 255:
                return False
        # Disallow 0.0.0.0 or 255.255.255.255 as host target
        if octets == [0, 0, 0, 0] or octets == [255, 255, 255, 255]:
            return False
        return True
    except (ValueError, TypeError):
        return False


def main():
    # Print guide to stderr so stdout remains clean for script piping if desired,
    # but print to stdout by default for interactive terminal users.
    sys.stderr.write(BANNER + "\n")
    sys.stderr.flush()

    while True:
        try:
            sys.stderr.write("Enter your Fire TV IP address (e.g. 192.168.1.50): ")
            sys.stderr.flush()
            
            raw_input = input().strip()
            
            if not raw_input:
                sys.stderr.write("  [!] Input cannot be empty. Please enter an IPv4 address.\n\n")
                continue

            if is_valid_ipv4(raw_input):
                valid_ip = raw_input
                sys.stderr.write(f"\n[+] Valid IPv4 address confirmed: {valid_ip}\n")
                sys.stderr.flush()
                # Machine-parseable output for terminal AI agents / scripts
                print(f"SUCCESS:{valid_ip}")
                sys.stdout.flush()
                sys.exit(0)
            else:
                sys.stderr.write(
                    f"  [X] '{raw_input}' is not a valid IPv4 address.\n"
                    "      Each of the 4 octets must be a number between 0 and 255.\n"
                    "      Example of a valid address: 192.168.0.125\n\n"
                )
                sys.stderr.flush()

        except (KeyboardInterrupt, EOFError):
            sys.stderr.write("\n\nOperation cancelled by user. Exiting.\n")
            sys.exit(130)


if __name__ == "__main__":
    main()
