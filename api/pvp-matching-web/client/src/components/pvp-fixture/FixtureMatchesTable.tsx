import {IRegisterMatchOutput} from "./PvpFixtureController";
import {Table} from "antd";
import React, {useState} from "react";
import DateUtils from "../../utils/DateUtils";

type Props = {
    data: IRegisterMatchOutput[];
    setSelected: (registeredIds: number[]) => void;
};

const FixtureMatchesTable = (props: Props) => {
    const columns = [
        {
            title: 'Match ID',
            dataIndex: 'registeredId',
            key: 'registeredId'
        },
        {
            title: 'Players',
            dataIndex: 'userNames',
            key: 'userNames'
        },
        {
            title: 'Mode',
            dataIndex: 'mode',
            key: 'mode',
            render: (mode: number) => modeToText(mode)
        },
        {
            title: 'Zone',
            dataIndex: 'zone',
            key: 'zone',
            render: (zone: string) => fixedZoneToText(zone)
        },
        {
            title: 'Hero',
            dataIndex: 'heroProfile',
            key: 'heroProfile',
            render: (heroProfile: number) => heroProfileToText(heroProfile)
        },
        {
            title: 'Start (UTC+7)',
            dataIndex: 'fromTime',
            key: 'fromTime',
            render: (time: Date) => {
                return DateUtils.toStandardFormat(time);
            }
        },
        {
            title: 'End (UTC+7)',
            dataIndex: 'toTime',
            key: 'toTime',
            render: (time: Date) => {
                return DateUtils.toStandardFormat(time);
            }
        }
    ];

    const dataSource = props.data.map(d => {
        return {
            registeredId: d.registeredId,
            userNames: `[${d.userIds[0]}] ${d.userNames[0]}\n[${d.userIds[1]}] ${d.userNames[1]}`,
            mode: d.mode,
            heroProfile: d.heroProfile,
            zone: d.fixedZone,
            fromTime: d.fromTime,
            toTime: d.toTime
        };
    });

    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

    const handleRowSelect = (newSelected: React.Key[]) => {
        setSelectedRowKeys(newSelected);
        const ids = selectedRowKeys.map(k => parseInt(k.toString()));
        ids.push(...newSelected.map(k => parseInt(k.toString())));
        props.setSelected(ids);
    };

    return (
        <Table dataSource={dataSource} columns={columns} rowKey={"registeredId"}
               rowSelection={{selectedRowKeys, onChange: handleRowSelect}}
               pagination={{pageSize: 100}}
               style={{whiteSpace: 'pre'}}>
        </Table>
    );
};

function modeToText(mode: number) {
    switch (mode) {
        case 1:
            return "1 vs 1 Normal";
        case 16:
            return "BO3 Tournament";
        case 32:
            return "BO5 Tournament";
        case 64:
            return "BO7 Tournament";
        default:
            return "?";
    }
}

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

function heroProfileToText(heroProfile: number): string {
    switch (heroProfile) {
        case 1:
            return "Qualifier";
        case 2:
            return "Top 16+";
        default:
            return "Free";
    }
}

export default FixtureMatchesTable;