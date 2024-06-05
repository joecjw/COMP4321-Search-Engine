import React, { useState, useCallback, memo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchSearchResult, fetchKeywords } from "../api/api";
import Page from "./Page";
import KeywordList from "./KeywordList";
import { Box, Button, Typography } from "@mui/material";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select from "@mui/material/Select";
import Card from "@mui/material/Card";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import PageSkeleton from "./PageSkeleton";
import KeywordListSkeleton from "./KeywordListSkeleton";
import CircularProgress from "@mui/material/CircularProgress";

const SearchPanel = () => {
	const [query, setQuery] = useState("");
	const [mode, setMode] = useState("keyword");
	const [section, setSection] = useState("both");
	const [raw, setRaw] = useState(undefined);
	const [keywordPage, setKeywordPage] = useState(0);

	const {
		data: searchResult,
		refetch,
		isLoading,
		isFetching,
		isError,
		error,
	} = useQuery({
		queryKey: ["pages"],
		queryFn: () =>
			fetchSearchResult({
				query: query,
				mode: mode,
				section: section,
				raw: raw,
			}),
		enabled: false,
		refetchOnWindowFocus: false,
		retry: false,
	});

	const handleQueryChange = (e) => {
		if (e.target.value.includes('"')) {
			let queryArray = e.target.value.split('"');
			if (
				queryArray[0].trim() != "" ||
				queryArray[queryArray.length - 1].trim() != ""
			) {
				setMode("mix");
				setQuery(e.target.value);
			} else {
				setMode("phrase");
				var newQuery = e.target.value.substring(1, e.target.value.length - 1);
				setQuery(newQuery);
			}
		} else {
			setMode("keyword");
			setQuery(e.target.value);
		}
		setRaw(true);
	};

	const handleSectionChange = (e) => {
		setSection(e.target.value);
	};

	const handleSearchParas = useCallback((newSearchParas) => {
		document.getElementById("search-input").value = newSearchParas.query;
		document.getElementById("search-section").value = document.getElementById(
			`search-section-${newSearchParas.section}`
		);

		setQuery(newSearchParas.query);
		setMode(newSearchParas.mode);
		setSection(newSearchParas.section);
		setRaw(newSearchParas.raw);
	}, []);

	const {
		data: keywordsData,
		refetch: keywordsRefetch,
		isLoading: isKeywordsLoading,
		isFetching: isKeywordsFetching,
		isError: isKeywordsError,
		error: keywordsError,
	} = useQuery({
		queryKey: ["keywords"],
		queryFn: () => fetchKeywords({ keywordPage: keywordPage }),
		refetchOnWindowFocus: false,
		refetchOnmount: false,
		refetchOnReconnect: false,
		retry: false,
	});

	useEffect(() => {
		keywordsRefetch();
	}, [keywordPage]);

	const handleKeywordClick = useCallback((keyword) => {
		setQuery((query) => {
			query == ""
				? (document.getElementById("search-input").value = keyword)
				: (document.getElementById("search-input").value =
						query + " " + keyword);
			return query == "" ? keyword : query + " " + keyword;
		});
		setRaw(false);
	}, []);

	let keywordResult;
	if (isKeywordsLoading || isKeywordsFetching) {
		keywordResult = <KeywordListSkeleton />;
	} else if (isKeywordsError) {
		console.log(keywordsError);
		keywordResult = (
			<Typography
				component={"div"}
				sx={{
					color: "red",
					display: "flex",
					alignItems: "center",
					justifyContent: "center",
				}}
				variant="h6"
			>
				<ErrorOutlineIcon />
				{keywordsError?.message}
			</Typography>
		);
	} else {
		keywordsData?.status == "BAD_REQUEST"
			? (keywordResult = (
					<Typography
						component={"div"}
						sx={{
							color: "red",
							display: "flex",
							alignItems: "center",
							justifyContent: "center",
						}}
						variant="h6"
					>
						<ErrorOutlineIcon />
						{keywordsData?.message}
					</Typography>
			  ))
			: (keywordResult = (
					<KeywordList
						data={keywordsData}
						handleKeywordClick={handleKeywordClick}
					/>
			  ));
	}

	let result;

	if (isLoading || isFetching) {
		result = <PageSkeleton />;
	} else if (isError) {
		result = (
			<Typography
				component={"div"}
				sx={{
					color: "red",
					display: "flex",
					alignItems: "center",
					justifyContent: "center",
				}}
				variant="h6"
			>
				<ErrorOutlineIcon />
				{error?.message}
			</Typography>
		);
	} else {
		if (searchResult?.status) {
			result = (
				<Typography
					component={"div"}
					sx={{
						color: "red",
						display: "flex",
						alignItems: "center",
						justifyContent: "center",
					}}
					variant="h6"
				>
					<ErrorOutlineIcon />
					{searchResult?.message}
				</Typography>
			);
		} else {
			result = searchResult ? (
				<Box
					sx={{
						display: "flex",
						flexDirection: "column",
						alignItems: "center",
						gap: "15px",
						padding: "20px",
						margin: "10px",
						minWidth: "fit-content",
						width: "100%",
					}}
				>
					{searchResult && (
						<Typography component={"div"} variant="h6">
							Find {searchResult?.retrievedNumber} results. Returned in:
							{searchResult?.executionTime}
						</Typography>
					)}

					{searchResult["retrievedPageMap"] !== undefined ? (
						<>
							<Typography component={"div"} variant="h8">
								Exact Matches
							</Typography>
							{searchResult?.retrievedPageMap?.exactMatch?.map((pageProps) => (
								<Page
									key={pageProps.pageID}
									pageProps={pageProps}
									handleSearchParas={handleSearchParas}
								/>
							))}
							<Typography component={"div"} variant="h8">
								Non-Exact Matches
							</Typography>
							{searchResult?.retrievedPageMap?.nonExactMatch?.map(
								(pageProps) => (
									<Page
										key={pageProps.pageID}
										pageProps={pageProps}
										handleSearchParas={handleSearchParas}
									/>
								)
							)}
						</>
					) : (
						searchResult?.retrievedPageList?.map((pageProps) => (
							<Page
								key={pageProps.pageID}
								pageProps={pageProps}
								handleSearchParas={handleSearchParas}
							/>
						))
					)}
				</Box>
			) : undefined;
		}
	}

	return (
		<Card
			variant="outlined"
			sx={{
				display: "flex",
				flexDirection: "column",
				alignItems: "center",
				justifyContent: "center",
				width: "100%",
				minWidth: "fit-content",
				minHeight: "100%",
				gap: "20px",
				borderWidth: "3px",
				padding: "10px",
				paddingY: "15px",
				margin: "5px",
			}}
		>
			<Box
				sx={{
					display: "flex",
					flexDirection: "column",
					alignItems: "center",
					justifyContent: "flex-start",
					width: "100%",
					minWidth: "fit-content",
					minHeight: "100%",
				}}
			>
				<Typography variant="h7" component={"div"}>
					Tips: use double quotes("query phrase") to enable Phrase Search
				</Typography>
				<input id="search-input" onChange={handleQueryChange} />
			</Box>

			<Box
				sx={{
					display: "flex",
					flexDirection: "row",
					alignItems: "center",
					justifyContent: "center",
					width: "50%",
					gap: "30px",
				}}
			>
				<Box sx={{ minWidth: 250, height: "50px" }}>
					<FormControl fullWidth>
						<InputLabel>Section</InputLabel>
						<Select
							id="search-section"
							value={section}
							label="Section"
							onChange={handleSectionChange}
						>
							<MenuItem id="search-section-title" value={"title"}>
								Title
							</MenuItem>
							<MenuItem id="search-section-body" value={"body"}>
								Body
							</MenuItem>
							<MenuItem id="search-section-both" value={"both"}>
								Both
							</MenuItem>
						</Select>
					</FormControl>
				</Box>
				<Button
					name="confirm-search"
					variant="contained"
					onClick={() => {
						refetch();
					}}
					sx={{ minWidth: "250px", height: "50px", fontSize: "18px" }}
				>
					Search
				</Button>
			</Box>

			<Typography component={"div"} variant="h7">
				Keywords Page:
				<Button
					variant="text"
					sx={{ fontSize: "15px", color: "black" }}
					onClick={() => {
						setKeywordPage(keywordPage - 1);
					}}
				>
					{"<"}
				</Button>
				{keywordPage}/10
				<Button
					variant="text"
					sx={{ fontSize: "15px", color: "black" }}
					onClick={() => {
						setKeywordPage(keywordPage + 1);
					}}
				>
					{">"}
				</Button>
			</Typography>

			{keywordResult}
			{result}
		</Card>
	);
};

export default React.memo(SearchPanel);
