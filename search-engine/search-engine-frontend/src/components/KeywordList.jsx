import React, { useState } from "react";
import Chip from "@mui/material/Chip";
import Grid from "@mui/material/Grid";
import Card from "@mui/material/Card";
import { Typography } from "@mui/material";
import { useEffect } from "react";
import CircularProgress from "@mui/material/CircularProgress";

const KeywordList = ({ data, handleKeywordClick }) => {
	const [keywords, setKeywords] = useState(
		<CircularProgress variant="indeterminate" disableShrink />
	);

	const mapKeywords = async () => {
		return await data?.keywords?.map((keyword, index) =>
			keyword == "" ? (
				""
			) : (
				<Grid
					key={index}
					item
					xs={1}
					sx={{
						padding: "5px",
						margin: "5px",
						minWidth: "fit-content",
						minHeight: "fit-content",
					}}
				>
					<Chip
						key={index}
						label={keyword}
						variant="outlined"
						onClick={() => handleKeywordClick(keyword)}
						sx={{
							fontSize: "15px",
							fontWeight: "500",
							borderRadius: "6px",
							borderWidth: "2px",
							bgcolor: "white",
							color: "#424242",
							width: "100%",
							height: "50px",
						}}
					/>
				</Grid>
			)
		);
	};

	useEffect(() => {
		if (data?.keywords?.length == 0) {
			setKeywords(
				<Typography variant="h5">No Keywords In Database</Typography>
			);
		} else {
			mapKeywords()
				.then((result) => {
					setKeywords(result);
				})
				.catch((error) => {
					console.log(error);
					setKeywords(
						<Typography variant="h5">Keyword List Mapping Error</Typography>
					);
				});
		}
	}, []);

	return (
		<Card
			variant="outlined"
			sx={{
				display: "flex",
				flexDirection: "column",
				alignItems: "center",
				justifyContent: "center",
				bgcolor: "#eceff1",
				minHeight: "fit-content",
				width: "500px",
				padding: "10px",
			}}
		>
			<Grid
				className="keyword-grid"
				container
				direction="row"
				alignItems="center"
				justifyContent="center"
				sx={{
					height: "25vh",
					width: "100%",
				}}
			>
				{keywords}
			</Grid>
		</Card>
	);
};

export default React.memo(KeywordList);
