<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>


<html lang="en">
<head>
  	<%@ include file="/include/common-head.jsp" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>React in JSP Example</title>
    <!-- React & ReactDOM -->
    <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
    
    <!-- Babel for JSX transformation -->
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>

    
</head>
<body>

    <h1>Lottery Data in React</h1>

    <!-- This is where React will render -->
    <div id="root"></div>

    <!-- Your React code inside script tag -->
    <script type="text/babel">
        // React component to fetch and display data
        class App extends React.Component {
            constructor(props) {
                super(props);
                this.state = {
                    data: [],
                    loading: true,
                    error: null
                };
            }

            componentDidMount() {
                // Fetching JSON data via AJAX (replace with your actual URL)
                fetch('/getLotteryData')  // Example URL for your backend endpoint
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Network response was not ok');
                        }
                        return response.json();
                    })
                    .then(data => {
                        this.setState({ data: data, loading: false });
                    })
                    .catch(error => {
                        this.setState({ error: error.message, loading: false });
                    });
            }

            // Function to handle sorting by Date
            handleSort = () => {
                const { data, sortOrder } = this.state;
                const sortedData = [...data];

                // Sort based on date
                sortedData.sort((a, b) => {
                    const dateA = new Date(a.DD);
                    const dateB = new Date(b.DD);

                    // Sort ascending or descending based on current sortOrder
                    return sortOrder === 'asc' ? dateA - dateB : dateB - dateA;
                });

                // Toggle the sort order
                const newSortOrder = sortOrder === 'asc' ? 'desc' : 'asc';

                // Update state with sorted data and new sortOrder
                this.setState({
                    data: sortedData,
                    sortOrder: newSortOrder
                });
            };

            handleSubmit = (event) => {
                event.preventDefault();

                // Get the user's numbers and mega number from the form
                const { userNumbers, userMega } = this.state;

                // Update the state with the input numbers and mega number
                const newData = this.state.data.map((row) => {
                    const winningNumbers = row.WN.split(' ');

                    // Check if the user's numbers match the winning numbers
                    const matchedNumbers = winningNumbers.map((num) => {
                        return userNumbers.includes(num) ? 'lottery-matched' : '';
                    });

                    // Check if the user's mega number matches
                    const megaMatched = row.MB === userMega ? 'lottery-matched' : '';

                    return {
                        ...row,
                        matchedNumbers,
                        megaMatched
                    };
                });

                this.setState({ data: newData });
            };

            handleNumberChange = (event) => {
                const value = event.target.value;
                const numbers = value.split(' ').map(num => num.trim());
                this.setState({ userNumbers: numbers });
            };

            handleMegaChange = (event) => {
                this.setState({ userMega: event.target.value });
            };

            
            render() {

                const { data, loading, error } = this.state;

                if (loading) {
                    return <div>Loading...</div>;
                }

                if (error) {
                    return <div>Error: {error}</div>;
                }

                return (
					<div>
						<div>Search is not properly working at this time</div>
                        <form onSubmit={this.handleSubmit}>
                            <label>
                                Enter 5 Numbers (separated by spaces):
                                <input
                                    type="text"
                                    onChange={this.handleNumberChange}
                                    placeholder="e.g. 1 2 3 4 5"
                                />
                            </label>
                            <br />
                            <label>
                                Enter Mega Number:
                                <input
                                    type="text"
                                    onChange={this.handleMegaChange}
                                    placeholder="e.g. 6"
                                />
                            </label>
                            <br />
                            <button type="submit">Submit</button>
                        </form>

                    <table className="lottery-table">
						<thead>
							<tr className="lottery-tr">
								<th className="lottery-th lottery-th-link" onClick={this.handleSort}>Draw Date</th>
								<th className="lottery-th">Winning Numbers</th>
								<th className="lottery-th">Mega Ball</th>
							</tr>
						</thead>
						<tbody>
                            {data.map((draw, index) => (
                                <tr key={index} className="lottery-tr">
                                    <td className="lottery-td">{draw.DD}</td>
                                    <td className="lottery-td">
                                        {/* Map each winning number to its own ball */}
                                        {draw.WN.split(' ').map((num, idx) => {
											const matchedClass = draw.matchedNumbers && draw.matchedNumbers[idx] ? draw.matchedNumbers[idx] : ''; 
											return (
                                            <span key={idx} className={`lottery-ball lottery-winning ${matchedClass ? matchedClass : ''}`}>{num}</span>
											);
                                        })}
                                    </td>
                                    <td className="lottery-td"><span className={`lottery-ball lottery-mega {draw.megaMatched}`}>{draw.MB}</span></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
				</div>
	            )};
        }

        // Render the App component into the DOM element with id 'root'
        ReactDOM.render(<App />, document.getElementById('root'));
    </script>

</body>
</html>
