import QtQuick 2.5

Flickable {
    id: flickArea
    contentWidth: table.width

    property alias tableData: table.tableData

    Table {
        id: table
        tableData: sensorList
    }

    onTableDataChanged: {
        destroy();
    }
}
