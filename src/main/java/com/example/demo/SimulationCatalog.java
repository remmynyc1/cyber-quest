package com.example.demo;

import com.example.demo.SimModels.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** All employee-track training content. Written in plain language for non-IT staff. */
@Component
public class SimulationCatalog {

    private final Map<String, Scenario> scenarios = new LinkedHashMap<>();
    private final Map<String, Inspect> inspects = new LinkedHashMap<>();
    private final Map<String, PhishTemplate> templates = new LinkedHashMap<>();

    public SimulationCatalog() {
        buildScenarios();
        buildInspects();
        buildTemplates();
    }

    public Collection<Scenario> scenarios() { return scenarios.values(); }
    public Scenario scenario(String id) { return scenarios.get(id); }
    public Collection<Inspect> inspects() { return inspects.values(); }
    public Inspect inspect(String id) { return inspects.get(id); }
    public Collection<PhishTemplate> templates() { return templates.values(); }
    public PhishTemplate template(String id) { return templates.get(id); }

    // ---- builders ----
    private static Choice c(String id, String text, String next, int pts, boolean safe, String fb) {
        return new Choice(id, text, next, pts, safe, fb);
    }
    private static Node n(String id, String text, Choice... cs) { return new Node(id, text, List.of(cs)); }
    private void add(String id, String title, String cat, String intro, int xp, List<String> take, Node... nodes) {
        Map<String, Node> map = new LinkedHashMap<>();
        for (Node nd : nodes) map.put(nd.id(), nd);
        scenarios.put(id, new Scenario(id, title, cat, intro, xp, nodes[0].id(), map, take));
    }
    private static Part p(String id, String slot, String text, boolean flag, String why) {
        return new Part(id, slot, text, flag, why);
    }
    private void addInspect(String id, String title, String cat, String intro, String fromName, boolean phish,
                            int xp, List<String> take, Part... parts) {
        inspects.put(id, new Inspect(id, title, cat, intro, fromName, phish, xp, List.of(parts), take));
    }

    // ---- branching scenarios ----
    private void buildScenarios() {
        add("helpdesk-call", "The urgent call from \"IT\"", "Phone scams",
            "Your phone rings while you are in the middle of work.", 100,
            List.of("IT will never ask for your password, by phone, email or chat.",
                    "Hang up and call back on a number you already trust.",
                    "Report the attempt. The same caller is probably dialing your coworkers."),
            n("start", "A caller says: \"Hi, this is Mark from the IT help desk. We detected a virus on your computer. I need your password right now to remove it before it spreads.\" What do you do?",
                c("a", "Give him your password so the virus can be removed.", null, 0, false,
                    "The caller now controls your account. Real IT staff can fix problems without knowing your password. A scary \"virus\" story is a classic pressure tactic."),
                c("b", "Install the remote-access tool he asks you to download.", null, 0, false,
                    "Remote-access tools hand a stranger full control of your computer. Scammers pose as help desk staff to get exactly this."),
                c("c", "Say you will call back, hang up, and dial the help desk number from the company directory.", "verify", 10, true,
                    "Correct. Calling back on a number you already trust defeats the scam, however convincing the caller sounded."),
                c("d", "Hang up without telling anyone.", "verify", 5, true,
                    "Hanging up is a good instinct, but see the next step. Saying nothing leaves your coworkers exposed.")),
            n("verify", "You reach the real help desk. They have no employee named Mark and no ticket for you. What next?",
                c("a", "Report the call to the security team so they can warn others.", null, 10, true,
                    "Excellent. Attackers usually call many people in a row. One report can protect the whole company."),
                c("b", "Do nothing. You did not fall for it.", null, 3, true,
                    "You protected yourself, but the caller is probably still working through your coworkers. Reporting is what protects everyone.")));

        add("ceo-gift-cards", "Urgent request from the CEO", "Email and text fraud",
            "A message arrives that appears to come from a senior executive.", 100,
            List.of("Urgency, secrecy and gift cards or wire transfers are the signature of CEO fraud.",
                    "Check the full sender address, then confirm through a phone number or chat you already have.",
                    "Reporting a scam attempt is always the right call."),
            n("start", "You get a text and an email that appear to come from your CEO: \"I'm in meetings all day and can't talk. I need five $200 gift cards for a client today. Keep this between us and I'll reimburse you.\" The email address looks almost right.",
                c("a", "Buy the gift cards. It is the CEO, after all.", null, 0, false,
                    "Gift cards are untraceable, which is why scammers love them. Once the codes are sent, the money is gone."),
                c("b", "Reply to the email to ask which cards to buy.", "pressure", 3, false,
                    "Replying tells the scammer your mailbox is active. Expect more pressure."),
                c("c", "Look closely at the sender address, then call the executive on a number you already have.", "confirmed", 10, true,
                    "Correct. Verifying through a separate channel is the single best defense against impersonation.")),
            n("pressure", "The sender replies within a minute: \"Hurry, I'm counting on you. Just send me photos of the card codes.\" What now?",
                c("a", "Send the photos. You have come this far.", null, 0, false,
                    "The money is gone. Pressure that grows when you hesitate is a warning sign, not a reason to hurry."),
                c("b", "Stop, and report the messages to security.", null, 7, true,
                    "Good recovery. It is never too late to stop and report.")),
            n("confirmed", "The executive's assistant says the CEO sent nothing. What next?",
                c("a", "Delete the messages and move on.", null, 4, true,
                    "You are safe, but security cannot warn others or block the sender if nobody tells them."),
                c("b", "Report the email and the text to security.", null, 10, true,
                    "Excellent. Your report helps block the sender and warns colleagues who may have received the same message.")));

        add("tailgating", "The stranger at the door", "Physical security",
            "You badge in at a side entrance during a busy morning.", 100,
            List.of("Everyone badges in, every time. Politely say so.",
                    "A uniform or full hands does not prove someone belongs here.",
                    "Report unbadged people to security rather than confronting them."),
            n("start", "Behind you, a person in a delivery vest with arms full of boxes says: \"Could you hold the door? My hands are full.\"",
                c("a", "Hold the door and let them through.", null, 0, false,
                    "Anyone you let in skips badge checks entirely. Following someone through a secured door is called tailgating."),
                c("b", "Politely explain they need to use the front desk or visitor entrance.", "later", 10, true,
                    "Correct. Being friendly and firm works. Delivery staff are used to it."),
                c("c", "Let them in but keep an eye on them.", "later", 3, false,
                    "Watching is not enough. You have now vouched for someone you cannot verify.")),
            n("later", "Later, you see someone in a delivery vest alone in the hallway near the server room, with no badge or visitor sticker. What do you do?",
                c("a", "Assume it is fine. They look busy.", null, 0, false,
                    "Looking busy is not the same as belonging. This is how thefts and tampering happen."),
                c("b", "Ask if you can help, and offer to walk them to reception.", null, 6, true,
                    "A decent approach, but alerting security first is safer, since you do not know who you are dealing with."),
                c("c", "Call security or the front desk with a description and location.", null, 10, true,
                    "Best option. Security can check quickly and safely.")));

        add("usb-drive", "The USB drive in the parking lot", "Malware",
            "Walking in from the car, you spot something on the ground.", 60,
            List.of("Never plug in a device you do not recognize, at work or at home.",
                    "Attackers deliberately leave tempting labels like \"Payroll\" or \"Confidential\".",
                    "Hand found devices to IT or security without plugging them in."),
            n("start", "You find a USB drive labeled \"Payroll 2026 - Confidential\" in the parking lot. What do you do?",
                c("a", "Plug it into your work computer to find out who owns it.", null, 0, false,
                    "This is exactly what the attacker hoped. Malware can install itself in seconds, on the company network."),
                c("b", "Plug it into your personal laptop at home to be safe.", null, 0, false,
                    "Your personal computer is not safe either, and it may later connect to company accounts."),
                c("c", "Hand it to IT or security without plugging it in.", null, 10, true,
                    "Correct. IT can inspect it in an isolated environment and check whether others found similar drives."),
                c("d", "Leave it where it is.", null, 4, true,
                    "Safe for you, but someone else might plug it in. Handing it to security is better.")));

        add("mfa-fatigue", "Login approvals at midnight", "Account takeover",
            "Your phone lights up late at night.", 100,
            List.of("Never approve a sign-in you did not start.",
                    "Repeated approval requests mean someone already has your password.",
                    "IT will never ask you to approve a prompt to \"make it stop\"."),
            n("start", "At 11:40 pm your phone shows \"Approve sign-in?\" You did not try to sign in. It buzzes again every minute. Then a text arrives: \"This is IT. Please approve the request so we can stop the alerts.\"",
                c("a", "Approve it to make the buzzing stop.", null, 0, false,
                    "The attacker is now logged in as you. This tactic is called MFA fatigue: wear you down until you give in."),
                c("b", "Deny each request, ignore it, and go to sleep.", "morning", 5, true,
                    "Denying is right, but nobody learns that your password may be stolen."),
                c("c", "Deny the requests and report it to IT or security right away.", "morning", 10, true,
                    "Correct. Repeated prompts you did not start mean your password is probably known to someone."),
                c("d", "Reply to the text to ask who is texting.", null, 0, false,
                    "Replying confirms your number is active. The text is part of the attack.")),
            n("morning", "The prompts stopped overnight. What do you do in the morning?",
                c("a", "Change your password and tell IT that someone may have it.", null, 10, true,
                    "Exactly right. Changing the password ends the attacker's access, and telling IT lets them check for damage."),
                c("b", "Nothing. It stopped, so it was probably a glitch.", null, 0, false,
                    "They stopped because the attacker gave up for now, not because the problem is gone. The password is still compromised.")));

        add("clicked-link", "I clicked it. Now what?", "Reporting",
            "You realize something was wrong, a few seconds too late.", 100,
            List.of("Everyone clicks sometimes. Speed of reporting is what limits the damage.",
                    "Report first. Then change your password from a different device if possible.",
                    "Honest, early reports are welcomed. Hiding mistakes is the only real failure."),
            n("start", "You clicked a link in an email, a page asked for your password, and you typed it in. Now you notice the web address looks wrong. What do you do?",
                c("a", "Close the tab and hope nothing happens.", null, 0, false,
                    "The attacker already has your password. Every hour you wait gives them more time."),
                c("b", "Tell IT or security immediately and change your password.", "followup", 10, true,
                    "Correct. Fast reports let security lock accounts and stop the attack, often before any harm is done."),
                c("c", "Wait a day to see whether anything strange happens.", null, 0, false,
                    "Waiting is the attacker's best friend. They may already be reading your email."),
                c("d", "Change your password but tell no one.", "followup", 5, true,
                    "Changing the password helps, but security needs to know to check for other damage.")),
            n("followup", "Security thanks you and asks what happened. How do you respond?",
                c("a", "Describe exactly what you clicked and entered, and when.", null, 10, true,
                    "Great. Accurate details help security find and fix the problem faster."),
                c("b", "Downplay it so you do not get in trouble.", null, 3, false,
                    "Incomplete information slows the response and can make the damage worse.")));

        add("invoice-fraud", "The new bank details", "Payment fraud",
            "You work in accounts payable, and a familiar supplier emails you.", 100,
            List.of("A change of bank details is the most common sign of invoice fraud.",
                    "Verify by calling the supplier on a number from your own records, not from the email.",
                    "Report suspected fraud to finance leadership and security."),
            n("start", "An email from a long-time supplier says: \"Our bank has changed. Please send this month's payment of $48,200 to the new account below.\" The invoice looks normal.",
                c("a", "Pay the invoice to the new account.", null, 0, false,
                    "The payment goes to a criminal. Recovering wire transfers is rarely possible."),
                c("b", "Reply to the email asking whether the new details are correct.", null, 2, false,
                    "If the mailbox is compromised or spoofed, the attacker simply replies \"yes\". Confirming through the same channel proves nothing."),
                c("c", "Call the supplier using the phone number in your vendor records.", "confirm", 10, true,
                    "Correct. A call to a number you already trust is the proven check.")),
            n("confirm", "The supplier says they never changed banks and never sent that email. What next?",
                c("a", "Report it to security and finance leadership.", null, 10, true,
                    "Excellent. They can check for similar emails and warn other staff."),
                c("b", "Ignore the email and move on.", null, 3, true,
                    "You avoided the loss, but the same attacker may target your colleagues next.")));
    }

    // ---- "spot the red flags" emails ----
    private void buildInspects() {
        addInspect("insp-payroll", "Payroll update email", "Phishing", "Read this email and tap anything that looks suspicious.",
            "HR Payroll", true, 80,
            List.of("Look at the domain after the @, not the display name.",
                    "Urgent deadlines and threats are designed to stop you from thinking.",
                    "Hover over links. Read the real domain, which is the part right before the first single slash."),
            p("from", "from", "payroll-support@hr-portal-update.com", true, "Genuine HR email comes from your company's own domain, not an unfamiliar one."),
            p("subj", "subject", "ACTION REQUIRED: confirm your direct deposit within 24 hours", true, "A tight deadline is meant to rush you into acting without checking."),
            p("greet", "body", "Dear Employee,", true, "Generic greetings are common in mass phishing. HR normally uses your name."),
            p("threat", "body", "Your direct deposit will be suspended unless you verify your bank details today.", true, "Threatening to cut off pay is an intimidation tactic."),
            p("link", "body", "https://acme-payroll.verify-account-secure.com/login", true, "The real domain is verify-account-secure.com. The company name is just a decoy at the front."),
            p("sig", "body", "Thank you, HR Team", false, "A signature alone proves nothing. Anyone can type a team name."));

        addInspect("insp-delivery", "Package delivery notice", "Phishing", "A delivery notice lands in your inbox. Tap anything that looks suspicious.",
            "Parcel Service", true, 80,
            List.of("If you were not expecting a package, treat the notice with suspicion.",
                    "Never open ZIP or Office attachments from unknown senders.",
                    "Small fees are used to steal card numbers."),
            p("from", "from", "tracking@pkg-delivery-notice.net", true, "Real carriers use their own well-known domains."),
            p("subj", "subject", "Your package could not be delivered", false, "This subject is generic, but it is not a red flag on its own. The details inside matter more."),
            p("body1", "body", "Hello customer, we attempted to deliver your parcel but nobody was home.", true, "It does not mention your name, the sender, or an order number."),
            p("fee", "body", "Pay a $1.99 redelivery fee at: http://pkg-redeliver.info/pay", true, "A small fee on an unfamiliar site is a common way to harvest card details."),
            p("att", "body", "Attachment: shipping_label.zip", true, "ZIP files from unknown senders often hide malware."));

        addInspect("insp-share", "Shared document notification", "Phishing", "A colleague seems to have shared a file with you. Look carefully before deciding.",
            "Mina Okafor via Docs", true, 100,
            List.of("Lookalike domains put the real brand early in the address, but the true domain is at the end.",
                    "A file you did not expect, even from a coworker, is worth a quick message to confirm.",
                    "Salary and bonus files are favorite bait."),
            p("from", "from", "no-reply@sharepoint-docs-review.co", true, "The domain is not your company's or Microsoft's official one, even though the name suggests it."),
            p("subj", "subject", "Mina shared \"Q4 Salaries and Bonuses.xlsx\" with you", true, "Tempting, confidential-sounding files are bait. Ask Mina directly before opening."),
            p("body1", "body", "Mina Okafor invited you to view this file.", false, "On its own this line is normal for a real share notification."),
            p("link", "body", "https://sharepoint.com.docs-review.co/q4-salaries", true, "The real domain is docs-review.co. \"sharepoint.com\" is only a prefix."),
            p("foot", "body", "This link will expire in 24 hours.", true, "Expiry pressure pushes you to click before thinking."));

        addInspect("insp-legit", "Team meeting agenda", "Legitimate email", "Not every email is a trick. Decide whether this one is safe.",
            "Dana Ruiz", false, 80,
            List.of("Good security is not about distrusting everything. It is about checking the right things.",
                    "Look for the pattern: known sender, expected content, no pressure, no odd links.",
                    "If in doubt, a quick message to the sender is always okay."),
            p("from", "from", "dana.ruiz@cyberquest.internal", false, "This is your company's own domain and a colleague you know."),
            p("subj", "subject", "Agenda for Thursday's team meeting", false, "Routine and expected."),
            p("greet", "body", "Hi team,", false, "A normal, informal greeting."),
            p("body1", "body", "The agenda is in the Team Meetings folder on the shared drive, same place as last week. No action needed beforehand.", false, "It points to a familiar location and asks for nothing risky."),
            p("sig", "body", "Thanks, Dana", false, "Nothing unusual."));
    }

    // ---- phishing campaign templates (managers launch these; employees see them in their inbox) ----
    private void buildTemplates() {
        templates.put("tpl-password", new PhishTemplate("tpl-password", "IT password expiry",
            "IT Service Desk", "it-helpdesk@cyberquest-support.com", "Your password expires in 24 hours",
            "Hello,\n\nYour network password will expire in 24 hours. To keep your account active, please confirm your current password using the link below.\n\nIT Service Desk",
            "Keep my password", "https://cyberquest-support.com/reset",
            List.of("The sender domain is cyberquest-support.com, not your company's domain.",
                    "The 24-hour deadline is pressure to act without checking.",
                    "Real password changes start from the company portal, never from an emailed link.")));
        templates.put("tpl-benefits", new PhishTemplate("tpl-benefits", "Benefits enrollment deadline",
            "Benefits Team", "benefits@cyberquest-hr.net", "Open enrollment closes today: confirm your selections",
            "Hello,\n\nOpen enrollment closes at 5 pm today. If you do not confirm your selections, your coverage will lapse.\n\nBenefits Team",
            "Confirm my selections", "https://cyberquest-hr.net/enroll",
            List.of("The domain cyberquest-hr.net is a lookalike, not the real HR system.",
                    "Threatening loss of coverage creates panic.",
                    "Check enrollment by opening the HR portal yourself, not through an email link.")));
        templates.put("tpl-shared", new PhishTemplate("tpl-shared", "Shared file from a coworker",
            "Dana Ruiz", "dana.ruiz@cyberquest-files.com", "Dana shared \"Bonus Schedule\" with you",
            "Dana Ruiz shared a file with you.\n\nBonus Schedule.xlsx",
            "Open file", "https://cyberquest-files.com/s/bonus",
            List.of("The display name says Dana, but the address belongs to cyberquest-files.com.",
                    "A sudden file about money is tempting bait.",
                    "Message Dana directly to ask whether she really sent it.")));
    }
}
