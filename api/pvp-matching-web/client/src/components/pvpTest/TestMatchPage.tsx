import {Button, Space} from "antd";
import React, {Dispatch, useEffect, useReducer, useState} from "react";
import TestMatchesTable from "./TestMatchesTable";
import PvpTestController, {IRegisterMatchTestData} from "./PvpTestController";

type Props = {
    registerMatchData: string[];
    resetSelection: boolean;
    dispatchCmd: (dispatch: Dispatch<Cmd>) => void;
};

const initState: IState = {
    refreshList: true
};

const TestMatchPage = (props: Props) => {
    const [matchIdsSelected, setMatchIdsSelected] = useState<string[]>([]);
    const [matches, setMatches] = useState<IRegisterMatchTestData[]>([]);
    const [cmdState, setCmdState] = useReducer(reducer, initState);

    useEffect(() => {
        props.dispatchCmd(setCmdState);
    }, [setCmdState, props.dispatchCmd]);

    useEffect(() => {
        if (!cmdState.refreshList) {
            return;
        }
        handleRefreshList().catch(console.error);
        setCmdState(Cmd.RefreshDone);
    }, [cmdState]);

    useEffect(() => {
        if (props.resetSelection) {
            setMatchIdsSelected([]);
        }
    }, [props.resetSelection]);

    const handleBtnUnregisterClicked = () => {
        if (matchIdsSelected.length === 0) {
            return;
        }
        new PvpTestController().unregisterMatch(matchIdsSelected).then(async () => {
            await handleRefreshList();
            setMatchIdsSelected([]);
        });
    };
    const handleMatchesSelected = (ids: string[]) => {
        setMatchIdsSelected(ids);
    }

    const handleRefreshList = async () => {
        const result = await new PvpTestController().getRegisteredMatches();
        if (!result || result.length === 0) {
            setMatches([]);
            return;
        }
        const uniqueMatch: IRegisterMatchTestData[] = []
        const ids = new Set<number>();
        result.flat().map(r => {
            if (r.id && !ids.has(r.id)) {
                uniqueMatch.push(r);
                ids.add(r.id);
            }
        })

        setMatches(uniqueMatch);
    }

    const hasSelected = matchIdsSelected.length > 0;

    return (
        <>
            <h2>Match List</h2>
            <Space direction={"horizontal"} style={{marginBottom: 16}}>
                <Button onClick={handleBtnUnregisterClicked} disabled={!hasSelected}>Delete</Button>
                <Button onClick={handleRefreshList}>Refresh</Button>
            </Space>
            <TestMatchesTable data={matches} setSelected={handleMatchesSelected}/>
        </>
    );
};

export default TestMatchPage;

const reducer = (state: IState, action: Cmd) => {
    switch (action) {
        case Cmd.Refresh:
            return {...state, refreshList: true};
        case Cmd.RefreshDone:
            return {...state, refreshList: false};
        default:
            throw new Error("Invalid action");
    }
}

interface IState {
    refreshList: boolean;
}

export enum Cmd {
    Refresh,
    RefreshDone
}