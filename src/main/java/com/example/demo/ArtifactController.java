package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/quest/artifacts")
public class ArtifactController {

    @GetMapping("/{challengeId}")
    public ResponseEntity<byte[]> downloadArtifact(@PathVariable Long challengeId) {
        // Challenge 1: Port Recon Alert
        if (challengeId == 1) {
            String portAlert = """
ALERT: Suspicious ingress SYN sequence detected on edge interface
SRC: 198.51.100.44
DST: 203.0.113.10
TARGET_SERVICE: Legacy Telnet Daemon (Port 23)
FLAGGED: Insecure cleartext protocol probe from untrusted WAN
STATUS: DROPPED
""";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"telnet_probe_alert.txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(portAlert.getBytes(StandardCharsets.UTF_8));
        }

        // Challenge 2: IAM Fail Stream
        if (challengeId == 2) {
            String iamStream = """
2026-10-09T14:10:01Z [IAM-ALERT] FAILED_LOGIN user=cfo@cyberquest.internal host=192.168.12.130 attempts=12
2026-10-09T14:10:04Z [IAM-ALERT] FAILED_LOGIN user=vp-eng@cyberquest.internal host=192.168.12.130 attempts=15
2026-10-09T14:10:08Z [IAM-ALERT] FAILED_LOGIN user=admin@cyberquest.internal host=192.168.12.130 attempts=20
""";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auth_fail_stream.txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(iamStream.getBytes(StandardCharsets.UTF_8));
        }

        // Challenge 3: SSH Brute Force
        if (challengeId == 3) {
            String authLog = """
Oct 09 14:22:01 edge-server sshd[14201]: Failed password for invalid user admin from 203.0.113.88 port 48212 ssh2
Oct 09 14:22:03 edge-server sshd[14203]: Failed password for invalid user root from 203.0.113.88 port 48214 ssh2
Oct 09 14:22:05 edge-server sshd[14205]: Failed password for invalid user support from 203.0.113.88 port 48216 ssh2
Oct 09 14:22:07 edge-server sshd[14207]: Failed password for invalid user ubnt from 203.0.113.88 port 48218 ssh2
Oct 09 14:22:09 edge-server sshd[14209]: Accepted password for deploy from 192.168.12.50 port 51230 ssh2
Oct 09 14:22:11 edge-server sshd[14211]: Failed password for invalid user test from 203.0.113.88 port 48220 ssh2
Oct 09 14:22:14 edge-server sshd[14215]: Failed password for invalid user oracle from 203.0.113.88 port 48222 ssh2
""";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auth_audit.log\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(authLog.getBytes(StandardCharsets.UTF_8));
        }

        // Challenge 4: Phishing EML
        if (challengeId == 4) {
            String rawEmail = """
Delivered-To: operative@cyberquest.internal
Received: by 10.0.0.1 with SMTP id a21; Fri, 09 Oct 2026 14:30:10 -0400
Return-Path: <spoofed-hr@payroll-security-update.com>
Received: from mail.attacker-relay.net (mail.attacker-relay.net [198.51.100.99])
Authentication-Results: cyberquest.internal;
 dkim=fail reason="signature missing";
 spf=fail (cyberquest.internal: domain of payroll-security-update.com does not designate 198.51.100.99 as permitted sender)
From: "Internal HR Portal" <hr@cyberquest.internal>
To: operative@cyberquest.internal
Subject: URGENT: Action Required on Employee 401(k) Record
X-Originating-IP: [198.51.100.99]

Operative, please confirm your tax identity payload at:
http://login.cyberquest-internal-auth.net/verify
""";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"phishing_investigation.eml\"")
                    .contentType(MediaType.parseMediaType("message/rfc822"))
                    .body(rawEmail.getBytes(StandardCharsets.UTF_8));
        }

        // Challenge 5: DNS PCAP
        if (challengeId == 5) {
            byte[] rawPcap = new byte[] {
                (byte)0xd4, (byte)0xc3, (byte)0xb2, (byte)0xa1, 0x02, 0x00, 0x04, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                (byte)0xff, (byte)0xff, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
                0x50, (byte)0xa0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x48, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00,
                0x00, 0x0c, 0x29, 0x3e, 0x5b, 0x71, 0x00, 0x50, 0x56, (byte)0xc0, 0x00, 0x08, 0x08, 0x00,
                0x45, 0x00, 0x00, 0x3a, 0x1c, 0x4d, 0x40, 0x00, 0x40, 0x11, 0x6e, (byte)0x9c,
                (byte)192, (byte)168, 0x0c, 0x10, (byte)198, 0x51, 0x64, 0x02,
                (byte)0xd0, 0x12, 0x00, 0x35, 0x00, 0x26, 0x00, 0x00,
                0x10, 0x24, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x0a, 'c', '2', '-', 'k', 'e', 'y', '-', '9', '9', '2',
                0x0a, 'c', 'y', 'b', 'e', 'r', 'q', 'u', 'e', 's', 't', 0x00, 0x00, 0x10, 0x00, 0x01
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dns_exfil_traffic.pcap\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(rawPcap);
        }

        return ResponseEntity.notFound().build();
    }
}