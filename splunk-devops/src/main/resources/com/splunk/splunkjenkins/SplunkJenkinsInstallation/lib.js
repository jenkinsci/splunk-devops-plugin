function checkDatasourceKey(event) {
    item=event.target
    var configItem = item.closest("[name=metadataItemSet]")
    if (configItem==null){
        return
    }
    var tableNode = configItem.querySelector(".splunk-meta-config-value")
    if (item.value == "disabled") {
        tableNode.style.display = "none"
        tableNode.nextSibling.style.display = "none"
    } else {
        tableNode.style.display = ""
        tableNode.nextSibling.style.display = "";
    }
}

Behaviour.specify(".splunk-meta-config-item", 'splk-config-item-check', 0, function (ele) {
    ele.onchange = checkDatasourceKey
});