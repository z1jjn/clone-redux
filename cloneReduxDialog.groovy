// Initialize libraries
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
@BaseScript CustomEndpointDelegate delegate
@JiraAgileBean
SprintIssueService sprintIssueService

// Initialize REST Endpoint
cloneReduxDialog(groups: ["jira-users"]) { MultivaluedMap queryParams ->
    
// Get the issue key
def issueKey = queryParams.getFirst("issueKey") as String
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    
// Initialize classes
IssueManager issueManager = ComponentAccessor.getComponent(IssueManager.class)
Issue issue = issueManager.getIssueObject(issueKey)
IssueLinkManager issueLinkManager = ComponentAccessor.getComponent(IssueLinkManager.class)
IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class)
CustomFieldManager customFieldManager = ComponentAccessor.getComponent(CustomFieldManager.class)
    
// Get date values
def getStartDayValue, getEndDayValue, getDueDateValue
getStartDayValue = customFieldManager.getCustomFieldObject("customfield_15313").getValue(issue)
getEndDayValue = customFieldManager.getCustomFieldObject("customfield_15314").getValue(issue)
getDueDateValue = issue.getDueDate()
    
// If non-system issue link value is greater than 1, display clone issue links.
def onlySystemLink
int countLinks, countSystemLinks = 0
    
issueLinkManager.getInwardLinks(issue.getId()).each{ inLinks ->
switch(inLinks.getLinkTypeId().toString())
	{
		case "10100":
        countSystemLinks++
        break
        case "10001":
        countSystemLinks++
        break
        case "10800":
        countSystemLinks++
        break
        case "10801":
        countSystemLinks++
        break
        default:
        countLinks++
        break
     }
}

issueLinkManager.getOutwardLinks(issue.getId()).each{ outLinks ->
switch(outLinks.getLinkTypeId().toString())
	{
		case "10100":
        countSystemLinks++
        break
        case "10001":
        countSystemLinks++
        break
        case "10800":
        countSystemLinks++
        break
        case "10801":
        countSystemLinks++
        break
        default:
        countLinks++
        break
        }
}
onlySystemLink = (countLinks >= 1) ? "false" : "true"

// Check if sub-tasks, attachments, issue links, dates exist in the issue
boolean getSubTasks, getAttachments, getIssueLinks, getSprintValues, getFixVersions, getAffectedVersions, getDates, showDates
getSubTasks = issue.getSubTaskObjects() ? true : false
getAttachments = issue.getAttachments() ? true : false
getIssueLinks = (onlySystemLink.equals("false")) ? true : false
getSprintValues = (!(sprintIssueService.getActiveOrFutureSprintForIssue(currentUser,issue).getValue() as String).equals("none()")) ? true : false
getFixVersions = issue.getFixVersions() ? true : false
getAffectedVersions = issue.getAffectedVersions() ? true : false
getDates = (getStartDayValue || getEndDayValue || getDueDateValue) ? true : false
    
// Check whether to clone sub-tasks, attachments, issue links, dates
def cloneSubTasks, cloneAttachments, cloneIssueLinks, cloneSprintValues, cloneDates, cloneFixVersions, cloneAffectedVersions
cloneAttachments = getAttachments ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-attachments"><label for="clone-redux-clone-attachments">Clone attachments</label>""" : ""
cloneIssueLinks = getIssueLinks ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-issue-links"><label for="clone-redux-clone-issue-links">Clone issue links</label>""" : ""
cloneDates = getDates ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-dates"><label for="clone-redux-clone-dates">Clone dates</label>""" : ""
cloneFixVersions = getFixVersions ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-fix-versions"><label for="clone-redux-clone-fix-versions">Clone fix version/s</label>""" : ""
cloneAffectedVersions = getAffectedVersions ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-affected-versions"><label for="clone-redux-clone-affected-versions">Clone affects version/s</label>""" : ""

if(!issue.issueType.isSubTask())
	{
        cloneSprintValues = getSprintValues ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-sprint-values"><label for="clone-redux-clone-sprint-values">Clone sprint values</label>""" : ""
    	cloneSubTasks = getSubTasks ? """<input class="checkbox" type="checkbox" id="clone-redux-clone-subtasks"><label for="clone-redux-clone-subtasks">Clone sub-tasks</label>""" : ""
    }
else
    {
        cloneSprintValues =""
        cloneSubTasks = ""
    }
    
// Dialog box pre-processor
def cloneReduxDialog = """
<script>
// Remove the DOM when the user presses escape
AJS.\$("#clone-redux-dialog").on('keydown', function(event) {
   if (event.key == "Escape")
       {
            e.preventDefault();
            AJS.dialog2("#clone-redux-dialog").remove();
       }
});

// Remove the DOM when the user clicks outside Clone Redux dialog
window.addEventListener('click', function(e){
    if (document.getElementById('clone-redux-dialog').contains(e.target))
        {
       		AJS.\$.noop
        }
    else
        {
            e.preventDefault();
            AJS.dialog2("#clone-redux-dialog").remove();
        }
});

// Clone when the user presses enter
var summaryInput = document.getElementById('clone-redux-issue-summary');
summaryInput.addEventListener('keyup', function(e) {
	if (event.keyCode === 13)
		{
			e.preventDefault();
       		AJS.\$("#clone-redux-submit").click();
     	}
});

// Removes the DOM when the user clicks Cancel
AJS.\$("#clone-redux-close-dialog").on('click', function (e) {
e.preventDefault();
AJS.dialog2("#clone-redux-dialog").remove();
});
    
// Pass the value to the processing REST Endpoint when the user clicks Clone
AJS.\$("#clone-redux-submit").on('click', function(e) {
e.preventDefault();
var cloneSummary = AJS.\$("#clone-redux-issue-summary").val()

// Check whether the summary has more than 1 character ignoring whitespace. If so, continue. Otherwise, show error.
if(cloneSummary.trim().length >= 1)
	{
    	// Check whether which clone value is checked
		var cloneSubTasks, cloneAttachments, cloneIssueLinks, cloneSprintValues, cloneDates, clonedIssue, cloneFlag, cloneFixVersions, cloneAffectedVersions
        cloneSubTasks = AJS.\$('#clone-redux-clone-subtasks').is(':checked') ? "true" : "false"
    	cloneAttachments = AJS.\$('#clone-redux-clone-attachments').is(':checked') ? "true" : "false"
     	cloneIssueLinks = AJS.\$('#clone-redux-clone-issue-links').is(':checked') ? "true" : "false"
    	cloneSprintValues = AJS.\$('#clone-redux-clone-sprint-values').is(':checked') ? "true" : "false"
    	cloneDates = AJS.\$('#clone-redux-clone-dates').is(':checked') ? "true" : "false"
        cloneFixVersions = AJS.\$('#clone-redux-clone-fix-versions').is(':checked') ? "true" : "false"
        cloneAffectedVersions = AJS.\$('#clone-redux-clone-affected-versions').is(':checked') ? "true" : "false"
        
		// Send ajax request to the processing REST Endpoint
        var request = AJS.\$.ajax({
        url: "/rest/scriptrunner/latest/custom/cloneRedux",
        method: "GET",
        data: { 
    	"cloneSummary": cloneSummary,
    	"issueId": """+issue.getId()+""",
        "cloneSubTasks": cloneSubTasks,
        "cloneAttachments": cloneAttachments,
        "cloneIssueLinks": cloneIssueLinks,
        "cloneSprintValues": cloneSprintValues,
        "cloneDates": cloneDates,
        "cloneFixVersions": cloneFixVersions,
        "cloneAffectedVersions": cloneAffectedVersions,
    	},
        beforeSend: function(){
        
        window.onbeforeunload = null;
        
        AJS.\$("#clone-redux-submit").prop("disabled", true);
        AJS.\$("#clone-redux-close-dialog").prop("disabled", true);
        cloneFlag = AJS.flag({
            type: 'info',
            close: 'never',
            title: 'Cloning',
            body: "Please do not leave or refresh the page.",
    	});
    	},
        success: function(data){
        window.location.replace(data)
        },
        error: function(e){
        cloneFlag.close()
        AJS.\$("#clone-redux-submit").prop("disabled", false);
        AJS.\$("#clone-redux-close-dialog").prop("disabled", false);
        AJS.flag({
            type: 'error',
            close: 'auto',
            title: 'Error',
            body: "Reloading the page in 5 seconds.",
    	});
 		setInterval(function() {
   		window.location.reload(true)
  		}, 5000);
    	}
    	});
    }
    else
        {
            AJS.flag({
                type: 'error',
                close: 'auto',
                title: 'Error',
                body: "You must specify a summary of the issue.",
        	});
        }
    });
</script>

// Main dialog box code
<section id="clone-redux-dialog" class="aui-dialog2 aui-dialog2-medium aui-layer" data-aui-modal="true" data-aui-remove-on-hide="true" role="dialog" aria-hidden="true">
	<header class="aui-dialog2-header">
		<h2 class="aui-dialog2-header-main">Clone Issue</h2>
    </header>
    <div class="aui-dialog2-content">
        <form class="aui"  onSubmit="return false;">
            <div class="field-group">
                <label for="clone-redux-issue-summary">Summary<span class="aui-icon icon-required">(required)</span></label>
                <input class="text medium-long-field" type="text" id="clone-redux-issue-summary" value="CLONE - """+issue.getSummary()+"""" onfocus="this.select()" autofocus>
            </div>
            <fieldset class="group">
                <div class="checkbox">
                    """+cloneSubTasks+"""
                </div>
                <div class="checkbox">
                    """+cloneAttachments+"""
                </div>
                <div class="checkbox">
                    """+cloneIssueLinks+"""
                </div>
                <div class="checkbox">
                    """+cloneSprintValues+"""
                </div>
                <div class="checkbox">
                    """+cloneDates+"""
                </div>
                <div class="checkbox">
                    """+cloneFixVersions+"""
                </div>
                <div class="checkbox">
                    """+cloneAffectedVersions+"""
                </div>
            </fieldset>
        </form>
    </div>
    <footer class="aui-dialog2-footer">
        <div class="aui-dialog2-footer-actions">
            <button accesskey="s" id="clone-redux-submit" class="aui-button aui-button-primary" title="Press Alt+s to submit this form.">Clone</button>
            <button id="clone-redux-close-dialog" class="aui-button aui-button-link">Cancel</button>
        </div>
        <div class="aui-dialog2-footer-hint">Clone Redux 1.1.1-stable.</div>
    </footer>
</section>
"""
// Display the Clone Redux dialog
Response.ok().type(MediaType.TEXT_HTML).entity(cloneReduxDialog.toString()).build()
}