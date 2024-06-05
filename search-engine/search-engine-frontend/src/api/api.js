export const fetchKeywords = async (keywordsParas) => {
	console.log(`fetch keywordPage ${keywordsParas.keywordPage}`);
	const response = await fetch(
		"http://localhost:8080/keywords?" + new URLSearchParams(keywordsParas)
	);
	const fetchKeywordsResult = await response.json();
	return fetchKeywordsResult;
};

export const fetchSearchResult = async (searchParas) => {
	console.log(searchParas);
	const response = await fetch(
		"http://localhost:8080/search?" + new URLSearchParams(searchParas)
	);
	const searchResult = await response.json();
	return searchResult;
};

export const fetchCrawlResult = async (crawlParas) => {
	console.log(crawlParas);
	const response = await fetch(
		"http://localhost:8080/crawl?" + new URLSearchParams(crawlParas)
	);
	const crawlResult = await response.json();
	return crawlResult;
};
