import {Table} from "antd";
import React, {useState} from "react";
import {IRegisterMatchTestData} from "./PvpTestController";

type Props = {
    data: IRegisterMatchTestData[];
    setSelected: (registeredIds: string[]) => void;
};

const TestMatchesTable = (props: Props) => {
    const columns = [
        {
            title: 'Id',
            dataIndex: 'id',
            key: 'id'
        },
        {
            title: 'User 1',
            dataIndex: "user1",
            key: 'user1'
        },
        {
            title: 'User 2',
            dataIndex: 'user2',
            key: 'user2',

        },
        {
            title: 'Zone',
            dataIndex: 'zone',
            key: 'zone',
            render: (zone: string) => fixedZoneToText(zone)
        },
    ];

    const flattenedData = props.data.flat();

    const dataSource = flattenedData.map(d => {
        return {
            id: d.id,
            user1: d.user1,
            user2: d.user2,
            zone: d.zone,
        };
    });

    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

    const handleRowSelect = (newSelected: React.Key[]) => {
        setSelectedRowKeys(newSelected);
        const ids = [];
        ids.push(...newSelected.map(k => parseInt(k.toString())));
        console.log(JSON.stringify(ids));
        const user: string[] = [];
        ids.map(id => {
            props.data.map((d, index) => {
                if (d.id === id) {
                    user.push(d.user1);
                    user.push(d.user2);
                }
            });
        });
        props.setSelected(user);
    };

    return (
        <Table dataSource={dataSource} columns={columns} rowKey={"id"}
               rowSelection={{selectedRowKeys, onChange: handleRowSelect}}
               pagination={{pageSize: 100}}
               style={{whiteSpace: 'pre'}}>
        </Table>
    );
};


function fixedZoneToText(fixedZone: string): string {
    switch (fixedZone) {
        case "sg":
            return "Singapore";
        case "br":
            return "Brazil";
        case "jp":
            return "Japan";
        case "us":
            return "USA";
        case "de":
            return "Germany";
        default:
            return "Free"
    }
}


export default TestMatchesTable;