import './App.css';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Table from 'react-bootstrap/Table';

import { useRef, useState, useEffect } from 'react';

import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min";

const bffUrl = '/api'
//const bffUrl = 'http://localhost:8081'

function App() {

  const [activeDeliveriesData, setActiveDeliveriesData] = useState([])
  const [completedDeliveriesData, setCompletedDeliveriesData] = useState([])
  const [searchData, setSearchData] = useState([])
  const [basketData, setBasketData] = useState([])

  const foodSearch = useRef(null);
  const customerId = useRef(null);
  const address = useRef(null);

  function fetchDeliveries() {

    fetch(bffUrl  + '/deliveries')
      .then(async response => {
        const data = await response.json();

        // check for error response
        if (!response.ok) {
            // get error message from body or default to response statusText
            const error = (data && data.message) || response.statusText;
            return Promise.reject(error);
        }
        
        console.log(data)
        
        var active = []
        var completed = []
        
        data.forEach(delivery => {
            if (delivery.status === 'CANCELED' || delivery.status === 'FOOD_DELIVERED') {
              completed.push(delivery)
            } else {
              active.push(delivery)
            }
        })

        setActiveDeliveriesData(active)
        setCompletedDeliveriesData(completed)
      
      })
      .catch(error => {
          console.error('There was an error!', error);
      });
  }

  
  useEffect(() => {
    fetchDeliveries()
  }, []);
  
  function handleFoodSearch(e) {
    e.preventDefault();
    console.log('Searching for meals with phrase: ' + foodSearch.current.value)

    fetch(bffUrl + '/foods?q=' + foodSearch.current.value)
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

    const farms = ['Always fresh fruits and veggies', 'Old McDonald', 'Farmstead Fare', 'Garden\'s Finest', 'Fresh-Picked Goods', 'Ranch-Raised']

    var requestBody = {}
    requestBody.customerId = customerId.current.value
    requestBody.address = address.current.value
    requestBody.items = basketData

    const random = Math.floor(Math.random() * farms.length);
    requestBody.farmId = farms[random]

    requestBody.deliveryCharge = Math.round(((1 + 9 * Math.random()) + Number.EPSILON) * 100) / 100;

    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    }

    fetch(bffUrl + '/orders', requestOptions)
        .then(async response => {
            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson && await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response status
                const error = (data && data.message) || response.status;
                return Promise.reject(error);
            }

            console.log('Order created.')
            console.log(data)
            // fetchDeliveries()
            window.location.reload();
        })
        .catch(error => {
            this.setState({ errorMessage: error.toString() });
            console.error('There was an error!', error);
        });
  }

  function changeDeliveryStatus(e) {
    e.preventDefault();
    console.log('Changing delivery status...')

    const index = e.target.value.lastIndexOf('_')
    const orderId = e.target.value.slice(0, index)
    const status = e.target.value.slice(index + 1)
    var url = ""
    var requestOptions

    if (status === 'cancel') {
      url = bffUrl + '/orders/' + orderId + '/status/cancel'
      requestOptions = {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({"reason": "Canceled by user"})
      }
    } else {
      url = bffUrl + '/deliveries/' + orderId
      requestOptions = {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({"status": status})
      }
    }

    

    fetch(url, requestOptions)
        .then(async response => {
            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson && await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response status
                const error = (data && data.message) || response.status;
                return Promise.reject(error);
            }

            console.log('Delivery updated')
            console.log(data)
            window.location.reload();
        })
        .catch(error => {
            this.setState({ errorMessage: error.toString() });
            console.error('There was an error!', error);
        });
  }

  function updateDeliveryMan(e) {
    e.preventDefault();
    console.log('Changing delivery man...')
    
    const index = e.target.value.lastIndexOf('_')
    const orderId = e.target.value.slice(0, index)
    const deliveryMan = e.target.value.slice(index + 1)

    var requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({"deliveryManId": deliveryMan})
    }

    fetch(bffUrl + '/deliveries/' + orderId + '/delivery-man', requestOptions)
        .then(async response => {
            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson && await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response status
                const error = (data && data.message) || response.status;
                return Promise.reject(error);
            }

            console.log('Delivery man updated')
            console.log(data)
            window.location.reload();
        })
        .catch(error => {
            this.setState({ errorMessage: error.toString() });
            console.error('There was an error!', error);
        });
  }

  function addTip(e) {
    e.preventDefault();
    console.log('Adding tip...')
    
    const index = e.target.value.lastIndexOf('_')
    const orderId = e.target.value.slice(0, index)
    const tip = e.target.value.slice(index + 1)

    var requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({"tip": parseFloat(tip)})
    }

    fetch(bffUrl + '/orders/' + orderId + '/tip', requestOptions)
        .then(async response => {
            const isJson = response.headers.get('content-type')?.includes('application/json');
            const data = isJson && await response.json();

            // check for error response
            if (!response.ok) {
                // get error message from body or default to response status
                const error = (data && data.message) || response.status;
                return Promise.reject(error);
            }

            console.log('Tip added')
            console.log(data)
            window.location.reload();
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
          <Col><h1>Farm-To-Table 🍒🥦🍓🍈🧀🥚</h1></Col>
        </Row>

        <Row>
          <Col><h3>Find products 🔎</h3></Col>
        </Row>
        <Form>
          <Row>
            <Col className="searchFood">
              <Form.Group className="mb-3" controlId="formBasicFood">
                <Form.Control placeholder="Enter product name" className="searchFoodContent" ref={foodSearch} />
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
                <th>Price [€]</th>
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
          <Col><h3>Current order 🧺</h3></Col>
        </Row>
        <Row>
          <h5>Products</h5>
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Name</th>
                <th>Price [€]</th>
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

        <Row>
          <Col><h3>Active deliveries 🚚</h3></Col>
        </Row>
        <Row className="foodSearchResults">
          <h5>Products</h5>
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Customer</th>
                <th>Farm</th>
                <th>Delivery Man</th>
                <th>Status</th>
                <th>Address</th>
                <th>Food</th>
                <th>Delivery charge [€]</th>
                <th>Tip [€]</th>
                <th>Total [€]</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {activeDeliveriesData.map((delivery, i) => (
                    <tr key={i}>
                        <td>{delivery.customerId}</td>
                        <td>{delivery.farmId}</td>
                        <td>
                          <Form.Select aria-label="Default select example" onChange={updateDeliveryMan}>
                            <option>{delivery.deliveryManId}</option>
                            <option value={delivery.orderId + "_fastest-guy"}>fastest-guy</option>
                            <option value={delivery.orderId + "_nicest-guy"}>nicest-guy</option>
                            <option value={delivery.orderId + "_cheapest-guy"}>cheapest-guy</option>
                          </Form.Select>
                        </td>
                        <td>{delivery.status}</td>
                        <td>{delivery.address}</td>
                        <td>{delivery.items.map(i => <div><b>{i.name}</b>, {i.amount} pieces, {i.pricePerItem} €/piece</div>)}</td>
                        <td>{delivery.deliveryCharge}</td>
                        <td>
                          <Form.Select aria-label="Default select example" onChange={addTip}>
                            <option>{delivery.tip}</option>
                            <option value={delivery.orderId + "_2.99"}>2.99</option>
                            <option value={delivery.orderId + "_5.99"}>5.99</option>
                            <option value={delivery.orderId + "_9.99"}>9.99</option>
                          </Form.Select>
                        </td>
                        <td>{delivery.total}</td>
                        <td>
                          <Form.Select aria-label="Default select example" onChange={changeDeliveryStatus}>
                            <option>Select action</option>
                            <option value={delivery.orderId + "_cancel"}>Cancel order</option>
                            <option value={delivery.orderId + "_prepareFood"}>Food in preparation</option>
                            <option value={delivery.orderId + "_foodReady"}>Food is ready</option>
                            <option value={delivery.orderId + "_pickUpFood"}>Food is picked up</option>
                            <option value={delivery.orderId + "_deliverFood"}>Food delivered</option>
                          </Form.Select>
                        </td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>

        <Row>
          <Col><h3>Completed deliveries 🍽</h3></Col>
        </Row>
        <Row className="foodSearchResults">
          <h5>Meals</h5>
          <Table striped bordered hover>
            <thead>
              <tr>
                <th>Customer</th>
                <th>Farm</th>
                <th>Delivery Man</th>
                <th>Status</th>
                <th>Address</th>
                <th>Food</th>
                <th>Delivery charge [€]</th>
                <th>Tip [€]</th>
                <th>Total [€]</th>
              </tr>
            </thead>
            <tbody>
              {completedDeliveriesData.map((delivery, i) => (
                    <tr key={i}>
                        <td>{delivery.customerId}</td>
                        <td>{delivery.farmId}</td>
                        <td>{delivery.deliveryManId}</td>
                        <td>{delivery.status}</td>
                        <td>{delivery.address}</td>
                        <td>{delivery.items.map(i => <div><b>{i.name}</b>, {i.amount} pieces, {i.pricePerItem} €/piece</div>)}</td>
                        <td>{delivery.deliveryCharge}</td>
                        <td>{delivery.tip}</td>
                        <td>{delivery.total}</td>
                    </tr>
                ))}
            </tbody>
          </Table>
        </Row>

      </Container>
    </div>
  );
}

export default App;
