import React, { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchSearchResult } from "../api/api";
import UrlList from "./UrlList";
import { Box, Button, Paper } from "@mui/material";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Divider from "@mui/material/Divider";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import SourceIcon from "@mui/icons-material/Source";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import ArrowCircleRightOutlinedIcon from "@mui/icons-material/ArrowCircleRightOutlined";
import AutoStoriesOutlinedIcon from "@mui/icons-material/AutoStoriesOutlined";

const Page = ({ pageProps, handleSearchParas }) => {
	const {
		pageID,
		title,
		size,
		lastModification,
		url,
		topKFreq,
		parentUrls,
		childUrls,
		simScore,
	} = pageProps;

	const [showParent, setShowParent] = useState(false);
	const [showChild, setShowChild] = useState(false);

	return (
		<Paper
			elevation={12}
			square={false}
			sx={{
				display: "flex",
				flexDirection: "column",
				gap: "8px",
				padding: "15px",
				margin: "5px",
				marginY: "12px",
				width: "55%",
				minWidth: "fit-content",
			}}
			className="page"
		>
			<Box
				sx={{
					display: "flex",
					flexDirection: "row",
					alignItems: "center",
					justifyContent: "center",
				}}
			>
				<Button
					variant="outlined"
					startIcon={
						<DoneAllIcon className="sim-icon" sx={{ color: "green" }} />
					}
					sx={{
						fontSize: "15px",
						fontWeight: "600",
						marginRight: "5px",
						color: "green",
						borderColor: "green",
						borderRadius: "20px",
					}}
				>
					{simScore.toFixed(4)}
				</Button>

				<Button
					href={url}
					sx={{ fontSize: "20px", fontWeight: "600", width: "80%" }}
				>
					{title}
				</Button>
			</Box>

			<Box
				sx={{
					minWidth: "fit-content",
					display: "flex",
					alignItems: "center",
					justifyContent: "space-evenly",
					gap: "5px",
				}}
			>
				<Button
					variant="outlined"
					href={url}
					startIcon={<ArrowCircleRightOutlinedIcon />}
					sx={{
						width: "50%",
						fontWeight: "900",
						fontSize: "15px",
						paddingY: "10px",
						margin: "3px",
						marginBottom: "4px",
					}}
				>
					Visit This Page
				</Button>
				<Button
					variant="outlined"
					startIcon={<AutoStoriesOutlinedIcon />}
					onClick={() => {
						let str = "";
						topKFreq.map((keyword) => {
							str += Object.keys(keyword)[0] + " ";
						});

						handleSearchParas({
							query: str.substring(0, str.length - 1),
							mode: "keyword",
							section: "both",
							raw: false,
						});
					}}
					sx={{
						width: "50%",
						fontWeight: "900",
						fontSize: "15px",
						paddingY: "10px",
						margin: "3px",
						marginBottom: "4px",
						minWidth:'fit-content'
					}}
				>
					Get Similar Pages
				</Button>
			</Box>

			<Divider />
			<Box sx={{ marginY: "4px" }}>
				<Stack direction="row" sx={{ marginY: "4px", gap: "5px" }}>
					<Chip
						icon={<CalendarMonthIcon />}
						label={lastModification.split("T")[0]}
						sx={{
							width: "50%",
							fontWeight: "600",
							margin: "3px",
							paddingY: "20px",
							color: "#424242",
							fontSize: "15px",
							bgcolor: "white",
							boxShadow: "2",
						}}
					/>
					<Chip
						icon={<SourceIcon />}
						label={parseInt(size) + " Bytes"}
						sx={{
							width: "50%",
							fontWeight: "600",
							margin: "3px",
							paddingY: "20px",
							color: "#424242",
							fontSize: "15px",
							bgcolor: "white",
							boxShadow: "2",
						}}
					/>
				</Stack>
				<Box
					sx={{
						display: "flex",
						flexDirection: "row",
						alignItems: "center",
						justifyContent: "space-evenly",
						minWidth: "fit-content",
						marginY: "4px",
						marginTop: "10px",
					}}
				>
					{topKFreq.map((keyword, index) => {
						return (
							<Button
								key={index}
								variant="contained"
								sx={{
									margin: "2px",
									marginX: "5px",
									fontSize: "13px",
									fontWeight: "600",
									paddingX: "5px",
									width: "20%",
									minWidth: "fit-content",
									color: "#424242",
									backgroundColor: "white",
									"&:hover": {
										backgroundColor: "white",
										color: "#424242",
									},
								}}
							>
								{Object.keys(keyword)[0]}
								<Chip
									label={Object.values(keyword)[0]}
									size={"small"}
									sx={{
										marginLeft: "3px",
										borderRadius: "4px",
										color: "#424242",
										padding: "0px",
										bgcolor: "#f5f5f5",
										minWidth: "fit-content",
									}}
								/>
							</Button>
						);
					})}
				</Box>
			</Box>

			<Button
				sx={{ fontWeight: "900", fontSize: "15px", margin: "3px" }}
				variant="contained"
				onClick={() => {
					setShowParent(!showParent);
				}}
			>
				Show Parent URLs
			</Button>
			{showParent && <UrlList urls={parentUrls} />}

			<Button
				sx={{ fontWeight: "900", fontSize: "15px", margin: "3px" }}
				variant="contained"
				onClick={() => {
					setShowChild(!showChild);
				}}
			>
				Show Child URLs
			</Button>
			{showChild && <UrlList urls={childUrls} />}
		</Paper>
	);
};

export default React.memo(Page);
