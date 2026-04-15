// Format memory bytes to human-readable format (B, KB, MB)
function formatMemory(bytes) {
    if (!bytes || bytes === 0) {
        return "0 B";
    }
    if (bytes < 1024) {
        return bytes + " B";
    } else if (bytes < 1024 * 1024) {
        return (bytes / 1024).toFixed(2) + " KB";
    } else {
        return (bytes / (1024 * 1024)).toFixed(2) + " MB";
    }
}

$(function () {
    $("body").on("click", ".view-code", function () {
        var id = $(this).attr("id");
        $.get(
            {
                url: "/api/status/view/" + id,
                success: function (data) {
                    var solution = data;
                    $(".prettyprint").attr("class", "prettyprint");
                    $("#modal-id").text(solution.id);
                    $("#modal-ce").text(solution.info || "");
                    $("#modal-username").text(solution.user ? solution.user.username : "Unknown");
                    $("#modal-problem").text(solution.problem ? solution.problem['id'] : "");
                    $("#modal-result").text(solution.normalResult || "");
                    $("#modal-language").text(solution.normalLanguage || "");
                    $("#modal-submit-time").text(solution.normalSubmitTime || "");
                    $("#modal-memory").text(formatMemory(solution.memory));
                    $("#modal-length").text(solution.length || 0);
                    $("#modal-time").text(solution.time || 0);
                    $("#source_code").text(solution.source || "");
                    PR.prettyPrint();
                    $("#codemodal").modal('show');
                    if (solution.share) {
                        $("#modal-share").text("Sharing");
                        $("#modal-share").attr("class", "ui button green");
                    } else {
                        $("#modal-share").text("Not Shared");
                        $("#modal-share").attr("class", "ui red button");
                    }
                },
                error: function (xhr, status, error) {
                    var errorMessage = "Failed to load solution";
                    if (xhr.status === 404) {
                        errorMessage = "Solution not found";
                    } else if (xhr.status === 403) {
                        errorMessage = "Permission denied";
                    } else if (xhr.status === 500) {
                        errorMessage = "Server error";
                    }
                    alert(errorMessage);
                    console.error("Error loading solution:", status, error);
                }
            }
        );
    });
    $("#modal-share").click(function () {
        $.post({
            url: "/api/status/share/" + $("#modal-id").text(),
            success: function (data) {
                if (data == true) {
                    $("#modal-share").text("Sharing");
                    $("#modal-share").attr("class", "ui button green");
                } else {
                    $("#modal-share").text("Not Shared");
                    $("#modal-share").attr("class", "ui button red");
                }
            },
            error: function (xhr, status, error) {
                var errorMessage = "Failed to update share status";
                if (xhr.status === 404) {
                    errorMessage = "Solution not found";
                } else if (xhr.status === 403) {
                    errorMessage = "Permission denied";
                }
                alert(errorMessage);
                console.error("Error updating share status:", status, error);
            }
        });
    });
});