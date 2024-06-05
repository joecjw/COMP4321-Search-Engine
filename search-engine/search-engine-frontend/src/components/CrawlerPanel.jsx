import React, { memo, useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchCrawlResult } from "../api/api";
import Card from "@mui/material/Card";
import { Typography, Button, Box } from "@mui/material";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import CircularProgress from "@mui/material/CircularProgress";

const CrawlerPanel = memo(() => {
	const [crawlParas, setCrawlParas] = useState(undefined);

	const {
		data: crawlResult,
		refetch,
		isLoading,
		isFetching,
		isError,
		error,
	} = useQuery({
		queryKey: ["crawl"],
		queryFn: () => fetchCrawlResult(crawlParas),
		enabled: false,
		refetchOnWindowFocus: false,
		refetchOnMount: false,
		refetchOnReconnect: false,
		retry: false,
	});

	useEffect(() => {
		if (crawlParas != undefined) {
			refetch();
		}
	}, [crawlParas]);

	const handleOnSubmit = (e) => {
		e.preventDefault();
		setCrawlParas({
			rootUrl: e.target.rootUrl.value,
			maxPages: e.target.maxPages.value,
		});
	};

	let result;

	if (isLoading || isFetching) {
		result = <CircularProgress />;
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
		console.log(crawlResult);
		if (crawlResult?.status) {
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
					{crawlResult?.message}
				</Typography>
			);
		} else {
			result = (
				<Box
					sx={{
						display: "flex",
						flexDirection: "column",
						alignItems: "baseline",
					}}
				>
					<Typography component={"div"} variant="h6">
						{crawlResult &&
							"Execution time: " + crawlResult?.executionTime + ".	"}
						{crawlResult &&
							"Total Crawled Pages: " +
								crawlResult?.crawlResult?.retrievedPageCount}
					</Typography>
				</Box>
			);
		}
	}

	return (
		<Card
			variant="outlined"
			sx={{
				display: "flex",
				flexDirection: "column",
				width: "100%",
				minHeight: "100%",
				minWidth: "fit-content",
				alignItems: "center",
				justifyContent: "flex-start",
				gap: "20px",
				padding: "15px",
				borderWidth: "3px",
			}}
		>
			<form className="form" onSubmit={handleOnSubmit}>
				<Box
					sx={{
						display: "flex",
						flexDirection: "column",
						width: "100%",
						height: "100%",
						alignItems: "center",
						justifyContent: "center",
					}}
				>
					<Box
						sx={{
							display: "flex",
							alignItems: "center",
							justifyContent: "center",
							width: "100%",
							minWidth: "fit-content",
							margin: "20px",
						}}
					>
						<Typography component={"div"} variant="h6">
							Root URL
						</Typography>
						<input
							id="crawl-input"
							name="rootUrl"
							type="text"
							defaultValue={
								"https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm"
							}
						/>
					</Box>
					<Box
						sx={{
							display: "flex",
							alignItems: "center",
							justifyContent: "center",
							width: "100%",
							minWidth: "fit-content",
							margin: "20px",
						}}
					>
						<Typography
							component={"div"}
							variant="h6"
							sx={{ textWrap: "nowrap" }}
						>
							Max. Number of Webpages
						</Typography>
						<input
							id="crawl-input"
							name="maxPages"
							type="text"
							defaultValue={300}
						/>
					</Box>
				</Box>

				<Button
					variant="contained"
					type="submit"
					sx={{
						minWidth: "fit-content",
						width: "60%",
						height: "50px",
						fontSize: "18px",
					}}
				>
					Crawl
				</Button>
			</form>
			<Box>{result}</Box>
		</Card>
	);
});

export default CrawlerPanel;
