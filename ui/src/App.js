import './App.css';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Table from 'react-bootstrap/Table';

import { useRef, useState } from 'react';

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min";

function App() {

  const [searchData, setSearchData] = useState([])
  const [basketData, setBasketData] = useState([])

  const foodSearch = useRef(null);
  const customerId = useRef(null);
  const address = useRef(null);
  
  function handleFoodSearch(e) {
    e.preventDefault();
    console.log('Searching for meals with phrase: ' + foodSearch.current.value)

    fetch('http://localhost:8081/foods?q=' + foodSearch.current.value)
        .then(async response => {
            const data = await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response statusText
                const error = (data && data.message) || response.statusText;
                return Promise.reject(error);
            }
            
            console.log(data)
            setSearchData(data)
        })
        .catch(error => {
            console.error('There was an error!', error);
        });
  }

  function addToBasket(e) {
    e.preventDefault();
    console.log('Adding to basket item with id: ' + e.target.id)
    // setBasketData([])
    console.log(basketData)

    const meal = searchData.find( ({id}) => id === e.target.id)
    console.log(meal)

    const mealInBasket = basketData.find( ({id}) => id === e.target.id)
    
    if (mealInBasket) {
      const result = []
      basketData.forEach(item => {
        if (item.id === meal.id) {
          item.amount = item.amount + 1
        }
        result.push(item)
      })

      setBasketData(result)

    } else {
      meal.amount = 1
      setBasketData([...basketData, meal])
    }
  }

  function removeFromBasket(e) {
    e.preventDefault();
    console.log('Removing item from basket with id: ' + e.target.id)

    var result = []
    
    basketData.forEach(item => {
      if (item.id === e.target.id) {
        console.log('Removing item from basket')
        console.log(item)
      } else {
        result.push(item)
      }
    })

    setBasketData(result)
  }

  function placeOrder(e) {
    e.preventDefault();
    console.log('Creating an order...')

    const restaurants = ['Pizza place', 'Kebab bar', 'Fast food chain', 'Meals like at home', 'Sushi bar', 'British pub']

    var requestBody = {}
    requestBody.customerId = customerId.current.value
    requestBody.address = address.current.value
    requestBody.items = basketData

    const random = Math.floor(Math.random() * restaurants.length);
    requestBody.restaurantId = restaurants[random]
    requestBody.deliveryCharge = 1 + 9 * Math.random()

    console.log(requestBody)

    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    }

    fetch('http://localhost:8081/orders', requestOptions)
        .then(async response => {
            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson && await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response status
                const error = (data && data.message) || response.status;
                return Promise.reject(error);
            }

            console.log(data)

            fetch('http://localhost:8081/deliveries')
              .then(async response => {
                const data = await response.json();

                // check for error response
                if (!response.ok) {
                    // get error message from body or default to response statusText
                    const error = (data && data.message) || response.statusText;
                    return Promise.reject(error);
                }
                
                console.log(data)

              
              })
        .catch(error => {
            console.error('There was an error!', error);
        });
        })
        .catch(error => {
            this.setState({ errorMessage: error.toString() });
            console.error('There was an error!', error);
        });
  }


  return (
    <div className="App">
      <Container>
        <Row className="header">
          <Col><h1>Food Delivery 🍉🥨🍗🥦🍙🍰</h1></Col>
        </Row>

        <Row>
          <Col><h3>Find meals</h3></Col>
        </Row>
        <Form>
          <Row>
            <Col className="searchFood">
              <Form.Group className="mb-3" controlId="formBasicFood">
                <Form.Control placeholder="Enter food name" className="searchFoodContent" ref={foodSearch} />
              </Form.Group>
            </Col>

            <Col className="searchFood">
              <Button variant="success" type="submit" className="searchFoodContent" onClick={handleFoodSearch}>
                Find
              </Button>
            </Col>
          </Row>
        </Form>
        <Row className="foodSearchResults">
          <h5>Results</h5>
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Name</th>
                <th>Price</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {searchData.map((item, i) => (
                    <tr key={i}>
                        <td>{item.name}</td>
                        <td>{item.pricePerItem}</td>
                        <td><Button variant="primary" type="submit" id={item.id} onClick={addToBasket}>Add</Button></td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>

        <Row>
          <Col><h3>Current order</h3></Col>
        </Row>
        <Row>
          <h5>Meals</h5>
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Name</th>
                <th>Price</th>
                <th>Amount</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {basketData.map((item, i) => (
                    <tr key={i}>
                        <td>{item.name}</td>
                        <td>{item.pricePerItem}</td>
                        <td>{item.amount}</td>
                        <td><Button variant="danger" type="submit" id={item.id} onClick={removeFromBasket}>Remove</Button></td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>
        <Row className="foodSearchResults">
          <Form className="formStyle">
            <Form.Group className="mb-3" controlId="formCustomerId">
              <Form.Label>Customer id</Form.Label>
              <Form.Control placeholder="Enter your id" ref={customerId} />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formAddress">
              <Form.Label>Address</Form.Label>
              <Form.Control placeholder="Enter your address" ref={address}/>
            </Form.Group>
            <Button variant="primary" type="submit" onClick={placeOrder}>
              Place an order
            </Button>
          </Form>
        </Row>

      </Container>
    </div>
  );
}

export default App;