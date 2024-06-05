import "./App.css";
import Home from "./components/Home";
import { Container } from "@mui/material";
import TopBar from "./components/TopBar";

function App() {
	return (
		<>
			<TopBar />
			<Container
				maxWidth="lg"
				sx={{
					minHeight: "fit-content",
					minWidth: "fit-content",
					height: "75vh",
					marginTop: "80px",
				}}
			>
				<Home />
			</Container>
		</>
	);
}

export default App;
