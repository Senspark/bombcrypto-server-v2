import {Button, Space} from "antd";
import React, {Dispatch, useEffect, useReducer, useState} from "react";
import PvpFixtureController, {IRegisterMatchOutput} from "./PvpFixtureController";
import FixtureMatchesTable from "./FixtureMatchesTable";

type Props = {
    registerMatchData: string[];
    resetSelection: boolean;
    dispatchCmd: (dispatch: Dispatch<Cmd>) => void;
};

const initState: IState = {
    refreshList: true
};

const FixtureMatchesPage = (props: Props) => {
    const [matchIdsSelected, setMatchIdsSelected] = useState<number[]>([]);
    const [matches, setMatches] = useState<IRegisterMatchOutput[]>([]);
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
        new PvpFixtureController().unregisterMatch(matchIdsSelected).then(async () => {
            await handleRefreshList();
            setMatchIdsSelected([]);
        });
    };
    const handleMatchesSelected = (ids: number[]) => {
        setMatchIdsSelected(ids);
    }

    const handleRefreshList = async () => {
        const result = await new PvpFixtureController().getRegisteredMatches();
        setMatches(result);
    }

    const hasSelected = matchIdsSelected.length > 0;

    return (
        <>
            <h2>Match List</h2>
            <Space direction={"horizontal"} style={{marginBottom: 16}}>
                <Button onClick={handleBtnUnregisterClicked} disabled={!hasSelected}>Delete</Button>
            </Space>
            <FixtureMatchesTable data={matches} setSelected={handleMatchesSelected}/>
        </>
    );
};

export default FixtureMatchesPage;

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