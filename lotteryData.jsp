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

    <h1 class="lottery-center">Lottery Data in React</h1>

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
                    error: null,
		            userNumbers: [], // Initialize as an empty array
        		    userMega: '',    // Initialize as an empty string
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
		                const dataWithShowRow = data.map(row => ({
        		            ...row,
                		    showRow: true // Initially set all rows to be shown
                		}));

                        this.setState({ data: dataWithShowRow, loading: false });
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

				// Check if both Winning Numbers and Mega number are empty
			    if (!userNumbers.length && !userMega) {
        			alert("Please enter at least one number (either Winning Numbers or Mega Number).");
        			return; // Prevent form submission if both are empty
    			}


			    // Normalize user numbers by stripping leading zeros
    			const normalizedUserNumbers = userNumbers.map(num => num.replace(/^0+/, ''));

                // Update the state with the input numbers and mega number
                const newData = this.state.data.map((row) => {
                	const winningNumbers = row.WN.split(' ');

			        // Normalize winning numbers by stripping leading zeros
    	   			const normalizedWinningNumbers = winningNumbers.map(num => num.replace(/^0+/, ''));

                    // Check if the user's numbers match the winning numbers
                    const matchedNumbers = normalizedWinningNumbers.map((num) => {
                        return normalizedUserNumbers.includes(num) ? 'lottery-matched' : '';
                    });

   				    // Check if the user's mega number matches
        			const normalizedUserMega = userMega.replace(/^0+/, '');  // Normalize mega ball number

			        // Normalize the winning mega ball number
        			const normalizedWinningMega = row.MB.replace(/^0+/, ''); // Normalize mega ball in winning data

        			const megaMatched = normalizedUserMega === normalizedWinningMega ? 'lottery-matched' : '';

		            // Determine if we should show the row based on at least one matched number
        		    const hasMatch = matchedNumbers.some((match) => match === 'lottery-matched') || (normalizedUserMega === normalizedWinningMega);


                    return {
                        ...row,
                        matchedNumbers,
                        megaMatched,
						showRow: hasMatch
                    };
                });

                this.setState({ data: newData });
            };

		    handleClear = () => {
        		// Reset the form and show all rows again
        		this.setState({
            		userNumbers: [],
            		userMega: '',
        		}, () => {
            		// After resetting the state, ensure all rows are shown
            		const resetData = this.state.data.map(row => ({
                		...row,
                		showRow: true, // Reset showRow to true for all rows
            		}));
            		this.setState({ data: resetData });
        		});
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

		        // Filter out the rows that should be visible (showRow = true)
        		const visibleRows = data.filter(row => row.showRow);

		        // Get the number of visible rows
        		const visibleRowCount = visibleRows.length;


                if (loading) {
                    return <div>Loading...</div>;
                }

                if (error) {
                    return <div>Error: {error}</div>;
                }

                return (
					<div>
                        <form className = "lottery-margin" onSubmit={this.handleSubmit}>
							<strong>Search Winning Numbers</strong>
							<br/>
                            <label>
                                Enter up to 5 Numbers (separated by spaces):
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
                            <button type="submit" className="lottery-button-spacing">Submit</button>
		                    <button type="button" onClick={this.handleClear}>Clear</button>

                        </form>

		                <div className="lottery-visible-rows">
        	            	{/* Display the number of visible rows */}
            	        	<strong>Visible Rows: {visibleRowCount}</strong>
                		</div>

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
									draw.showRow && (
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
									)
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
