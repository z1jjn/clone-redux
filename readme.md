tl;dr: Custom cloning solution for Jira Server 8.x. You need ScriptRunner to use this. Paste the codes to the REST Endpoint in ScriptRunner and create a new web object to display the dialog. It's been quite a while since I've coded and used this. Might not work outside the box.<br>
<br>
<strong>VERSION HISTORY</strong><br>
1.1.1 23 Mar 2022 
<ul><li>Added user guide link in the modal.</li></ul>
1.1.0 15 Jul 2021 
<ul><li>Added option to clone Fix & Affects Version/s.</li></ul>
1.0.0 15 Feb 2021 
<ul><li>First release.</li></ul>

<H3>WHAT IS CLONE REDUX?</H3>
Redux /ˌrēˈdəks/<br>
adjective<br>
1.	brought back; revived.[source]<br>
Clone Redux adds a functionality to clone issues with or without dates (Start Day, End Day, Due Date), you will be able to customize your cloning needs.<br>
<br>
<h3>FEATURES</h3>
<ul>
<li>Up to 67.81% reduction in cloning time* compared to the built-in cloning function</li>
<li>Cloning an existing ticket's Start Day</li>
<li>Cloning an existing ticket's End Day</li>
<li>Cloning an existing ticket's Due Date </li>
<li>Cloning an existing ticket's Fix Version 1.1.0</li>
<li>Cloning an existing ticket's Affect/s Version 1.1.0</li>
</ul>
<h3>USAGE</h3>
Using Clone Redux is as easy as using the built-in clone function. Use Clone Redux in both Standard Issue Types and Sub-Tasks.
The options in Clone Redux is displayed based on the ticket's details.
<ul>
<li>If a ticket has sub-tasks, Clone sub-tasks will be shown. </li>
  <ul><li>This is not shown if you are cloning a sub-task.</li></ul>
<li>If a ticket has attachments, Clone attachments will be shown.</li>
<li>If a ticket has non-system issue links, Clone issue links will be shown. </li>
  <ul><li>Just like the built-in cloning function, Clone issue links will NOT clone Confluence page links and Web links.</li></ul>
<li>If a ticket has ongoing and future sprint values, Clone sprint values will be shown. </li>
  <ul><li>This is not shown if you are cloning a sub-task because they automatically inherit their parent ticket's sprint values.</li></ul>
<li>If a ticket has Start Day, End Day, or Due Date, Clone dates will be shown.</li>
<li>If a ticket has Fix version/s, Clone fix version/s will be shown. 1.1.0 </li>
  <ul><li>Supports cloning one or multiple fix version/s.</li></ul>
<li>If a ticket has Affects version/s, Clone affects version/s will be shown. 1.1.0 </li>
  <ul><li>Supports cloning one or multiple affects version/s.</li></ul>
</ul>
<h3>LIMITATIONS</h3>
<ul>
<li>Autofocus only works once | AUI Limitation</li>
<li>Can't remove closed sprints for cloned tickets | JIRA Agile (formerly GreenHopper) API Limitation</li>
<li>Can clone only through More → Clone Redux | JIRA REST API Limitation</li>
<li>Breaks when cloning a ticket with quotation mark (") in the summary | ScriptRunner Limitation</li>
</ul>
<h3>TROUBLESHOOTING & FAQ</h3>
Q: An error toast appears when I click Clone.<br>
A: Once the error toast appears, you will be redirected to the original issue ticket after 5 seconds. The error toast meant something happened in the REST Endpoint preprocessor.<br>
Q: Nothing happens when I click Clone Redux.<br>
A: You may wait up to 5 seconds for the dialog box to appear.<br>
Q: I waited more than 10 seconds, still nothing happened.<br>
A: The ticket may have been deleted or moved. Please open the browser console (f12) and make sure status code 500 does not appear whenever you click Clone. If the status code is not present in the browser console, you may try the following:
<ul><li>do a hard refresh (ctrl+shift+r); or</li>
<li>clear your browsers cookies and cache; or</li>
<li>use Incognito/InPrivate mode.</li></ul>
